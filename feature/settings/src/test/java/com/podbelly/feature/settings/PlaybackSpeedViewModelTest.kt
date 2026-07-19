package com.podbelly.feature.settings

import app.cash.turbine.test
import com.podbelly.core.database.dao.PodcastDao
import com.podbelly.core.database.entity.PodcastEntity
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackSpeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val podcastDao = mockk<PodcastDao>(relaxed = true)
    private val customSpeedsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { podcastDao.getPodcastsWithCustomSpeed() } returns customSpeedsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makePodcast(id: Long, title: String, speed: Float) = PodcastEntity(
        id = id,
        feedUrl = "https://example.com/$id",
        title = title,
        author = "Author",
        description = "Description",
        artworkUrl = "https://art.com/$id.jpg",
        link = "",
        language = "en",
        lastBuildDate = 0L,
        subscribedAt = 0L,
        playbackSpeed = speed,
    )

    @Test
    fun `podcastSpeeds maps entities to speed items`() = runTest {
        customSpeedsFlow.value = listOf(
            makePodcast(id = 1L, title = "Fast Show", speed = 1.5f),
            makePodcast(id = 2L, title = "Slow Show", speed = 0.8f),
        )
        val viewModel = PlaybackSpeedViewModel(podcastDao)

        viewModel.podcastSpeeds.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val items = expectMostRecentItem()
            assertEquals(2, items.size)
            assertEquals(PodcastSpeedItem(1L, "Fast Show", "https://art.com/1.jpg", 1.5f), items[0])
            assertEquals(PodcastSpeedItem(2L, "Slow Show", "https://art.com/2.jpg", 0.8f), items[1])
        }
    }

    @Test
    fun `podcastSpeeds is empty when no podcast has a custom speed`() = runTest {
        val viewModel = PlaybackSpeedViewModel(podcastDao)

        viewModel.podcastSpeeds.test {
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(expectMostRecentItem().isEmpty())
        }
    }

    @Test
    fun `resetSpeed clears one podcast's custom speed`() = runTest {
        val viewModel = PlaybackSpeedViewModel(podcastDao)

        viewModel.resetSpeed(7L)
        advanceUntilIdle()

        coVerify { podcastDao.resetPlaybackSpeed(7L) }
    }

    @Test
    fun `resetAllSpeeds clears every custom speed`() = runTest {
        val viewModel = PlaybackSpeedViewModel(podcastDao)

        viewModel.resetAllSpeeds()
        advanceUntilIdle()

        coVerify { podcastDao.resetAllPlaybackSpeeds() }
    }
}
