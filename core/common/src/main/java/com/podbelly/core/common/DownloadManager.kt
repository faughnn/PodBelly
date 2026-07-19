package com.podbelly.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.podbelly.core.database.dao.DownloadErrorDao
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.entity.DownloadErrorEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages downloading podcast episode audio files to local storage.
 *
 * Downloads are saved to the app's external files directory under a "podcasts" subdirectory.
 * Progress is exposed via [downloadProgress] as a map of episodeId to progress (0.0 to 1.0).
 */
@Singleton
class DownloadManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val episodeDao: EpisodeDao,
    private val downloadErrorDao: DownloadErrorDao,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
) {

    private val _downloadProgress = MutableStateFlow<Map<Long, Float>>(emptyMap())

    /** Observable map of episodeId to download progress (0.0 - 1.0). */
    val downloadProgress: StateFlow<Map<Long, Float>> = _downloadProgress.asStateFlow()

    private val _downloadErrors = MutableSharedFlow<DownloadErrorEvent>(extraBufferCapacity = 5)

    /** Emits a one-shot event every time a download fails, so screens can show a Snackbar. */
    val downloadErrors: SharedFlow<DownloadErrorEvent> = _downloadErrors.asSharedFlow()


    /**
     * Returns `true` when the user has enabled WiFi-only downloads and the device is not
     * currently on WiFi. Callers should check this *before* launching a download so they
     * can show a user-facing warning instead of silently failing.
     */
    suspend fun isDownloadBlockedByWifiSetting(): Boolean {
        val wifiOnly = preferencesManager.downloadOnWifiOnly.first()
        return wifiOnly && !isOnWifi()
    }

    /**
     * Returns `true` when the user restricted *automatic* downloads to charging
     * sessions and the device isn't currently plugged in. Only the smart
     * auto-download paths consult this — a download the user starts by hand
     * always runs.
     */
    suspend fun isAutoDownloadBlockedByChargingSetting(): Boolean {
        val chargingOnly = preferencesManager.smartAutoDownloadChargingOnly.first()
        if (!chargingOnly) return false
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return false
        return !batteryManager.isCharging
    }

    /**
     * Queues auto-downloads for a show's newly inserted episodes, honouring the
     * "keep newest N per show" cap: only the newest [keepPerShow] arrivals are
     * queued, and older auto-downloaded, unplayed episodes beyond the cap are
     * deleted so the show's auto-downloads never pile up. Each queued episode is
     * flagged autoDownloaded first, which is what makes it eligible for that
     * cleanup later — manual downloads are untouched. [keepPerShow] <= 0 means
     * unlimited (no cap, no cleanup).
     */
    suspend fun autoDownloadNewEpisodes(
        podcastId: Long,
        inserted: List<AutoDownloadCandidate>,
        keepPerShow: Int,
    ) {
        // IGNORE-conflict inserts return -1 for rows that already existed.
        val fresh = inserted.filter { it.episodeId > 0L }
        val toQueue = if (keepPerShow > 0) {
            fresh.sortedByDescending { it.publicationDate }.take(keepPerShow)
        } else {
            fresh
        }
        toQueue.forEach { candidate ->
            episodeDao.markAutoDownloaded(candidate.episodeId)
            enqueueDownload(candidate.episodeId)
        }
        if (keepPerShow > 0) {
            // The just-queued episodes have no downloadPath yet, so shrink the
            // keep-window by their count: once they land, the show is at the cap.
            val remainingSlots = (keepPerShow - toQueue.size).coerceAtLeast(0)
            episodeDao.getAutoDownloadsBeyondNewest(podcastId, remainingSlots)
                .forEach { deleteDownload(it) }
        }
    }

    /**
     * Downloads the audio file for the given episode.
     *
     * The file is saved to `context.getExternalFilesDir("podcasts")/<episodeId>.mp3`.
     * On success, the episode's downloadPath, fileSize, and downloadedAt fields are updated in the database.
     *
     * @param episodeId The database primary key of the episode to download.
     * @throws IllegalStateException if the episode is not found in the database.
     */
    suspend fun downloadEpisode(episodeId: Long) = withContext(Dispatchers.IO) {
        val episode = episodeDao.getByIdOnce(episodeId)
            ?: throw IllegalStateException("Episode $episodeId not found in database")

        if (episode.audioUrl.isBlank()) {
            Log.w(TAG, "Episode $episodeId has no audio URL, skipping download")
            _downloadErrors.tryEmit(
                DownloadErrorEvent(episodeId, episode.title, "No audio URL available")
            )
            return@withContext
        }

        // Enforce WiFi-only setting
        val wifiOnly = preferencesManager.downloadOnWifiOnly.first()
        if (wifiOnly && !isOnWifi()) {
            Log.w(TAG, "WiFi-only download enabled but not on WiFi, skipping episode $episodeId")
            val msg = "WiFi required – connect to WiFi or disable in Settings"
            downloadErrorDao.insert(
                DownloadErrorEntity(
                    episodeId = episodeId,
                    errorMessage = msg,
                    errorCode = 0,
                    timestamp = System.currentTimeMillis(),
                )
            )
            _downloadErrors.tryEmit(DownloadErrorEvent(episodeId, episode.title, msg))
            // enqueueDownload optimistically seeded a 0% progress entry; clear it here
            // (as every other exit path does) so the UI doesn't show a stuck spinner
            // when the network flipped to mobile between enqueue and execution.
            _downloadProgress.update { it - episodeId }
            return@withContext
        }

        // Update progress to indicate download has started
        _downloadProgress.update { it + (episodeId to 0f) }

        try {
            val request = Request.Builder()
                .url(episode.audioUrl)
                .header("User-Agent", "Podbelly/1.0 (Android Podcast App)")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val code = response.code
                val msg = "HTTP $code"
                response.close()
                _downloadProgress.update { it - episodeId }
                // Transient server / rate-limit errors: throw so the IOException handler
                // records it and the WorkManager worker retries with backoff.
                if (code >= 500 || code == 408 || code == 429) {
                    throw IOException(msg)
                }
                // Permanent (4xx) failure: record and give up.
                Log.e(TAG, "Download failed with code $code for episode $episodeId")
                downloadErrorDao.insert(
                    DownloadErrorEntity(
                        episodeId = episodeId,
                        errorMessage = msg,
                        errorCode = code,
                        timestamp = System.currentTimeMillis(),
                    )
                )
                _downloadErrors.tryEmit(DownloadErrorEvent(episodeId, episode.title, msg))
                return@withContext
            }

            val body = response.body ?: run {
                Log.e(TAG, "Empty response body for episode $episodeId")
                response.close()
                _downloadProgress.update { it - episodeId }
                // Treat as transient so the worker retries.
                throw IOException("Server returned empty response")
            }

            val podcastsDir = context.getExternalFilesDir("podcasts")
                ?: throw IllegalStateException("External files directory not available")

            if (!podcastsDir.exists()) {
                podcastsDir.mkdirs()
            }

            val outputFile = File(podcastsDir, "$episodeId.mp3")
            val contentLength = body.contentLength()
            var totalBytesRead = 0L
            var lastPercent = -1

            // When the server omits Content-Length (chunked transfer encoding returns -1),
            // we can't compute a percentage. Surface an indeterminate sentinel so the UI
            // shows a moving spinner instead of a frozen 0% that looks like a hung download.
            if (contentLength <= 0) {
                _downloadProgress.update { it + (episodeId to INDETERMINATE_PROGRESS) }
            }

            body.byteStream().use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        // Cancellation checkpoint: blocking reads aren't suspension
                        // points, so without this a cancelled download would keep
                        // writing until the stream ends.
                        coroutineContext.ensureActive()
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Emit progress only when the whole-number percentage changes,
                        // not on every 8KB chunk (~thousands of Map allocations / file).
                        if (contentLength > 0) {
                            val percent = (totalBytesRead * 100 / contentLength).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _downloadProgress.update { it + (episodeId to percent / 100f) }
                            }
                        }
                    }

                    outputStream.flush()
                }
            }

            response.close()

            // Final cancellation checkpoint before persisting: if the download was
            // cancelled just as it finished, do not record a downloadPath in the DB
            // (the CancellationException handler removes the partial/complete file).
            coroutineContext.ensureActive()

            // Update database with download info
            episodeDao.setDownloadPath(
                id = episodeId,
                path = outputFile.absolutePath,
                fileSize = totalBytesRead,
                downloadedAt = System.currentTimeMillis(),
            )

            // Clear any previous download errors for this episode
            downloadErrorDao.deleteByEpisodeId(episodeId)

            // Mark download as complete and remove from progress tracking
            _downloadProgress.update { it - episodeId }

            Log.i(TAG, "Downloaded episode $episodeId (${totalBytesRead / 1024} KB) to ${outputFile.absolutePath}")

        } catch (e: CancellationException) {
            // Download was cancelled; clean up partial file
            val podcastsDir = context.getExternalFilesDir("podcasts")
            val partialFile = File(podcastsDir, "$episodeId.mp3")
            if (partialFile.exists()) {
                partialFile.delete()
            }
            _downloadProgress.update { it - episodeId }
            throw e
        } catch (e: IOException) {
            // Transient network/transfer failure (timeout, connection drop, etc.).
            // Record it and rethrow so the WorkManager worker can retry with backoff.
            Log.e(TAG, "Network error downloading episode $episodeId", e)
            deletePartialDownload(episodeId)
            val msg = e.message ?: "Network error"
            downloadErrorDao.insert(
                DownloadErrorEntity(
                    episodeId = episodeId,
                    errorMessage = msg,
                    errorCode = 0,
                    timestamp = System.currentTimeMillis(),
                )
            )
            _downloadErrors.tryEmit(DownloadErrorEvent(episodeId, episode.title, msg))
            _downloadProgress.update { it - episodeId }
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading episode $episodeId", e)
            deletePartialDownload(episodeId)
            val msg = e.message ?: "Unknown error"
            downloadErrorDao.insert(
                DownloadErrorEntity(
                    episodeId = episodeId,
                    errorMessage = msg,
                    errorCode = 0,
                    timestamp = System.currentTimeMillis(),
                )
            )
            _downloadErrors.tryEmit(DownloadErrorEvent(episodeId, episode.title, msg))
            _downloadProgress.update { it - episodeId }
        }
    }

    /**
     * Removes a partially-written download file. Called on failure paths so a download
     * that ultimately fails (after retries) doesn't leave an orphaned `.mp3` on disk —
     * the DB-driven delete paths never see it because no downloadPath was recorded.
     */
    private fun deletePartialDownload(episodeId: Long) {
        try {
            val podcastsDir = context.getExternalFilesDir("podcasts")
            val partialFile = File(podcastsDir, "$episodeId.mp3")
            if (partialFile.exists()) {
                partialFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete partial download for episode $episodeId", e)
        }
    }

    /**
     * Enqueues an episode download via WorkManager so it survives app switches.
     *
     * The download runs as a foreground worker with a persistent notification.
     * Uses [ExistingWorkPolicy.KEEP] so duplicate requests for the same episode are ignored.
     */
    fun enqueueDownload(episodeId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_EPISODE_ID to episodeId))
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .addTag(DOWNLOAD_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            "$DOWNLOAD_WORK_TAG:$episodeId",
            ExistingWorkPolicy.KEEP,
            request,
        )

        // Show immediate progress in UI while WorkManager starts up — but only when the
        // network constraint is already satisfiable. If we're offline, WorkManager defers
        // the worker indefinitely, downloadEpisode() never runs to clear this seed, and the
        // UI is left with a permanently stuck 0% spinner. downloadEpisode() seeds its own
        // 0% when it actually starts, so skipping the optimistic seed while offline is safe.
        if (hasNetwork()) {
            _downloadProgress.update { it + (episodeId to 0f) }
        }
    }

    /**
     * Cancels an in-progress download for the given episode.
     *
     * @param episodeId The database primary key of the episode whose download to cancel.
     */
    fun cancelDownload(episodeId: Long) {
        workManager.cancelUniqueWork("$DOWNLOAD_WORK_TAG:$episodeId")
        _downloadProgress.update { it - episodeId }

        // Do NOT delete the file here. WorkManager cancellation is asynchronous, so a
        // synchronous delete could race the still-writing worker and remove a file it
        // is about to record as "downloaded" — leaving a broken entry. The worker's own
        // CancellationException handler removes the partial file after it actually stops.
    }

    /**
     * Deletes a previously downloaded episode file and clears the download fields in the database.
     *
     * @param episodeId The database primary key of the episode whose download to delete.
     */
    suspend fun deleteDownload(episodeId: Long) = withContext(Dispatchers.IO) {
        val episode = episodeDao.getByIdOnce(episodeId) ?: return@withContext

        if (episode.downloadPath.isNotBlank()) {
            val file = File(episode.downloadPath)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    Log.w(TAG, "Failed to delete file: ${episode.downloadPath}")
                }
            }
        }

        episodeDao.clearDownload(episodeId)
        // Clear any recorded download errors so a stale failure doesn't linger in the
        // Downloads error list for an episode that no longer has a download.
        downloadErrorDao.deleteByEpisodeId(episodeId)
        Log.i(TAG, "Deleted download for episode $episodeId")
    }

    /**
     * Deletes all downloaded episode files and clears the download fields in the database.
     *
     * @return The number of downloads deleted.
     */
    suspend fun deleteAllDownloads(): Int = withContext(Dispatchers.IO) {
        val episodes = episodeDao.getDownloadedEpisodesOnce()
        var count = 0

        for (episode in episodes) {
            if (episode.downloadPath.isNotBlank()) {
                val file = File(episode.downloadPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            episodeDao.clearDownload(episode.id)
            downloadErrorDao.deleteByEpisodeId(episode.id)
            count++
        }

        Log.i(TAG, "Deleted all downloads ($count episodes)")
        count
    }

    private fun isOnWifi(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /** Returns `true` when an internet-capable network is currently active. */
    private fun hasNetwork(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val TAG = "DownloadManager"
        private const val BUFFER_SIZE = 8 * 1024 // 8 KB buffer
        private const val DOWNLOAD_WORK_TAG = "episode_download"

        /**
         * Sentinel progress value meaning "downloading, total size unknown" (server sent no
         * Content-Length). The UI renders an indeterminate spinner for any negative value.
         */
        const val INDETERMINATE_PROGRESS = -1f
    }
}

data class DownloadErrorEvent(
    val episodeId: Long,
    val episodeTitle: String,
    val message: String,
)
