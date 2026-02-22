package com.podbelly.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.podbelly.core.database.dao.DownloadErrorDao
import com.podbelly.core.database.dao.EpisodeDao
import com.podbelly.core.database.entity.EpisodeEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadManagerTest {

    private val okHttpClient = mockk<OkHttpClient>(relaxed = true)
    private val episodeDao = mockk<EpisodeDao>(relaxed = true)
    private val downloadErrorDao = mockk<DownloadErrorDao>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>()
    private val context = mockk<Context>()

    private val connectivityManager = mockk<ConnectivityManager>()
    private val mockNetwork = mockk<Network>()
    private val mockCapabilities = mockk<NetworkCapabilities>()

    private val testEpisode = EpisodeEntity(
        id = 1L,
        podcastId = 1L,
        guid = "guid-1",
        title = "Test Episode",
        description = "Description",
        audioUrl = "https://example.com/episode.mp3",
        publicationDate = 1000L,
        durationSeconds = 3600,
        artworkUrl = "",
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns mockNetwork
        every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
        coEvery { episodeDao.getByIdOnce(1L) } returns testEpisode
    }

    private fun createDownloadManager(): DownloadManager {
        return DownloadManager(
            okHttpClient = okHttpClient,
            episodeDao = episodeDao,
            downloadErrorDao = downloadErrorDao,
            preferencesManager = preferencesManager,
            context = context,
        )
    }

    @Test
    fun `downloadEpisode blocks when wifi-only enabled and not on wifi`() = runTest {
        every { preferencesManager.downloadOnWifiOnly } returns flowOf(true)
        every { mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false

        val downloadManager = createDownloadManager()
        downloadManager.downloadEpisode(1L)

        coVerify {
            downloadErrorDao.insert(match { it.errorMessage.contains("WiFi") })
        }
    }

    @Test
    fun `downloadEpisode blocks when wifi-only enabled and no network`() = runTest {
        every { preferencesManager.downloadOnWifiOnly } returns flowOf(true)
        every { connectivityManager.activeNetwork } returns null

        val downloadManager = createDownloadManager()
        downloadManager.downloadEpisode(1L)

        coVerify {
            downloadErrorDao.insert(match { it.errorMessage.contains("WiFi") })
        }
    }

    @Test
    fun `downloadEpisode proceeds when wifi-only disabled`() = runTest {
        every { preferencesManager.downloadOnWifiOnly } returns flowOf(false)
        every { mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        // Mock external files dir so the download flow continues past WiFi check
        every { context.getExternalFilesDir("podcasts") } returns null

        val downloadManager = createDownloadManager()

        try {
            downloadManager.downloadEpisode(1L)
        } catch (_: IllegalStateException) {
            // Expected: "External files directory not available" — but proves WiFi check was passed
        }

        // Should NOT have recorded a WiFi error
        coVerify(exactly = 0) {
            downloadErrorDao.insert(match { it.errorMessage.contains("WiFi") })
        }
    }

    @Test
    fun `downloadEpisode proceeds when wifi-only enabled and on wifi`() = runTest {
        every { preferencesManager.downloadOnWifiOnly } returns flowOf(true)
        every { mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { context.getExternalFilesDir("podcasts") } returns null

        val downloadManager = createDownloadManager()

        try {
            downloadManager.downloadEpisode(1L)
        } catch (_: IllegalStateException) {
            // Expected: "External files directory not available" — proves WiFi check passed
        }

        coVerify(exactly = 0) {
            downloadErrorDao.insert(match { it.errorMessage.contains("WiFi") })
        }
    }

    @Test
    fun `downloadEpisode skips when audio url is blank`() = runTest {
        coEvery { episodeDao.getByIdOnce(1L) } returns testEpisode.copy(audioUrl = "")

        val downloadManager = createDownloadManager()
        downloadManager.downloadEpisode(1L)

        // Should not attempt WiFi check or download
        coVerify(exactly = 0) { downloadErrorDao.insert(any()) }
    }

    @Test
    fun `downloadEpisode throws when episode not found`() = runTest {
        coEvery { episodeDao.getByIdOnce(999L) } returns null

        val downloadManager = createDownloadManager()

        try {
            downloadManager.downloadEpisode(999L)
            org.junit.Assert.fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            org.junit.Assert.assertTrue(e.message!!.contains("999"))
        }
    }

    @Test
    fun `cancelDownload removes from progress and cleans up`() = runTest {
        every { context.getExternalFilesDir("podcasts") } returns null

        val downloadManager = createDownloadManager()

        // Register a mock job
        val mockJob = mockk<kotlinx.coroutines.Job>(relaxed = true)
        downloadManager.registerDownloadJob(1L, mockJob)

        downloadManager.cancelDownload(1L)

        verify { mockJob.cancel() }
        // Progress should not contain the episode
        org.junit.Assert.assertFalse(downloadManager.downloadProgress.value.containsKey(1L))
    }

    @Test
    fun `deleteDownload clears DB fields`() = runTest {
        coEvery { episodeDao.getByIdOnce(1L) } returns testEpisode.copy(
            downloadPath = "/data/podcasts/1.mp3"
        )

        val downloadManager = createDownloadManager()
        downloadManager.deleteDownload(1L)

        coVerify { episodeDao.clearDownload(1L) }
    }

    @Test
    fun `deleteDownload does nothing when episode not found`() = runTest {
        coEvery { episodeDao.getByIdOnce(999L) } returns null

        val downloadManager = createDownloadManager()
        downloadManager.deleteDownload(999L)

        coVerify(exactly = 0) { episodeDao.clearDownload(any()) }
    }

    @Test
    fun `deleteAllDownloads clears all downloaded episodes`() = runTest {
        val episodes = listOf(
            testEpisode.copy(id = 1L, downloadPath = "/data/podcasts/1.mp3"),
            testEpisode.copy(id = 2L, guid = "guid-2", downloadPath = "/data/podcasts/2.mp3"),
        )
        coEvery { episodeDao.getDownloadedEpisodesOnce() } returns episodes

        val downloadManager = createDownloadManager()
        val count = downloadManager.deleteAllDownloads()

        org.junit.Assert.assertEquals(2, count)
        coVerify { episodeDao.clearDownload(1L) }
        coVerify { episodeDao.clearDownload(2L) }
    }

    @Test
    fun `deleteAllDownloads returns zero when no downloads exist`() = runTest {
        coEvery { episodeDao.getDownloadedEpisodesOnce() } returns emptyList()

        val downloadManager = createDownloadManager()
        val count = downloadManager.deleteAllDownloads()

        org.junit.Assert.assertEquals(0, count)
    }
}
