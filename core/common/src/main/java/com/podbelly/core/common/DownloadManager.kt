package com.podbelly.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.podbelly.core.database.dao.DownloadErrorDao
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.entity.DownloadErrorEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
) {

    private val _downloadProgress = MutableStateFlow<Map<Long, Float>>(emptyMap())

    /** Observable map of episodeId to download progress (0.0 - 1.0). */
    val downloadProgress: StateFlow<Map<Long, Float>> = _downloadProgress.asStateFlow()

    private val _downloadErrors = MutableSharedFlow<DownloadErrorEvent>(extraBufferCapacity = 5)

    /** Emits a one-shot event every time a download fails, so screens can show a Snackbar. */
    val downloadErrors: SharedFlow<DownloadErrorEvent> = _downloadErrors.asSharedFlow()

    /** Active download jobs, keyed by episode ID. */
    private val activeDownloads = mutableMapOf<Long, Job>()

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
                Log.e(TAG, "Download failed with code ${response.code} for episode $episodeId")
                val msg = "HTTP ${response.code}"
                downloadErrorDao.insert(
                    DownloadErrorEntity(
                        episodeId = episodeId,
                        errorMessage = msg,
                        errorCode = response.code,
                        timestamp = System.currentTimeMillis(),
                    )
                )
                _downloadErrors.tryEmit(DownloadErrorEvent(episodeId, episode.title, msg))
                response.close()
                _downloadProgress.update { it - episodeId }
                return@withContext
            }

            val body = response.body ?: run {
                Log.e(TAG, "Empty response body for episode $episodeId")
                _downloadErrors.tryEmit(
                    DownloadErrorEvent(episodeId, episode.title, "Server returned empty response")
                )
                response.close()
                _downloadProgress.update { it - episodeId }
                return@withContext
            }

            val podcastsDir = context.getExternalFilesDir("podcasts")
                ?: throw IllegalStateException("External files directory not available")

            if (!podcastsDir.exists()) {
                podcastsDir.mkdirs()
            }

            val outputFile = File(podcastsDir, "$episodeId.mp3")
            val contentLength = body.contentLength()
            var totalBytesRead = 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Update progress if content length is known
                        if (contentLength > 0) {
                            val progress = (totalBytesRead.toFloat() / contentLength.toFloat())
                                .coerceIn(0f, 1f)
                            _downloadProgress.update { it + (episodeId to progress) }
                        }
                    }

                    outputStream.flush()
                }
            }

            response.close()

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
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading episode $episodeId", e)
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
            .addTag(DOWNLOAD_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "$DOWNLOAD_WORK_TAG:$episodeId",
            ExistingWorkPolicy.KEEP,
            request,
        )

        // Show immediate progress in UI while WorkManager starts up
        _downloadProgress.update { it + (episodeId to 0f) }
    }

    /**
     * Cancels an in-progress download for the given episode.
     *
     * @param episodeId The database primary key of the episode whose download to cancel.
     */
    fun cancelDownload(episodeId: Long) {
        activeDownloads[episodeId]?.cancel()
        activeDownloads.remove(episodeId)
        WorkManager.getInstance(context).cancelUniqueWork("$DOWNLOAD_WORK_TAG:$episodeId")
        _downloadProgress.update { it - episodeId }

        // Clean up any partial file
        val podcastsDir = context.getExternalFilesDir("podcasts")
        val partialFile = File(podcastsDir, "$episodeId.mp3")
        if (partialFile.exists()) {
            partialFile.delete()
        }
    }

    /**
     * Retries a failed download, incrementing the retry count before attempting the download again.
     *
     * @param episodeId The database primary key of the episode to retry.
     */
    suspend fun retryDownload(episodeId: Long) {
        downloadErrorDao.incrementRetryCount(episodeId)
        downloadEpisode(episodeId)
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
            count++
        }

        Log.i(TAG, "Deleted all downloads ($count episodes)")
        count
    }

    /**
     * Registers an active download job so it can be cancelled later.
     * Call this when launching the download coroutine.
     */
    fun registerDownloadJob(episodeId: Long, job: Job) {
        activeDownloads[episodeId] = job
    }

    private fun isOnWifi(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        private const val TAG = "DownloadManager"
        private const val BUFFER_SIZE = 8 * 1024 // 8 KB buffer
        private const val DOWNLOAD_WORK_TAG = "episode_download"
    }
}

data class DownloadErrorEvent(
    val episodeId: Long,
    val episodeTitle: String,
    val message: String,
)
