package com.podbelly.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests for [PlaybackService] constants and structure.
 *
 * PlaybackService is a [MediaSessionService] annotated with no Hilt entry point
 * that creates an [ExoPlayer] internally in onCreate(). Full lifecycle testing
 * would require a Hilt test runner and complex service binding, so these tests
 * focus on verifiable aspects: companion object constants and class structure.
 */
class PlaybackServiceTest {

    // -------------------------------------------------------------------------
    // Custom command constants
    // -------------------------------------------------------------------------

    @Test
    fun `CUSTOM_COMMAND_SET_SKIP_SILENCE has expected value`() {
        assertEquals(
            "SET_SKIP_SILENCE",
            PlaybackService.CUSTOM_COMMAND_SET_SKIP_SILENCE
        )
    }

    @Test
    fun `CUSTOM_COMMAND_SET_VOLUME_BOOST has expected value`() {
        assertEquals(
            "SET_VOLUME_BOOST",
            PlaybackService.CUSTOM_COMMAND_SET_VOLUME_BOOST
        )
    }

    @Test
    fun `custom command constants are distinct`() {
        val commands = setOf(
            PlaybackService.CUSTOM_COMMAND_SET_SKIP_SILENCE,
            PlaybackService.CUSTOM_COMMAND_SET_VOLUME_BOOST,
        )
        assertEquals(
            "All custom command constants must be unique",
            2,
            commands.size
        )
    }

    @Test
    fun `custom command constants are non-empty strings`() {
        assert(PlaybackService.CUSTOM_COMMAND_SET_SKIP_SILENCE.isNotBlank()) {
            "CUSTOM_COMMAND_SET_SKIP_SILENCE must not be blank"
        }
        assert(PlaybackService.CUSTOM_COMMAND_SET_VOLUME_BOOST.isNotBlank()) {
            "CUSTOM_COMMAND_SET_VOLUME_BOOST must not be blank"
        }
    }

    // -------------------------------------------------------------------------
    // Class structure
    // -------------------------------------------------------------------------

    @Test
    fun `PlaybackService class exists and is loadable`() {
        val clazz = PlaybackService::class.java
        assertNotNull("PlaybackService class should be loadable", clazz)
    }

    @Test
    fun `PlaybackService extends MediaSessionService`() {
        val superclass = PlaybackService::class.java.superclass
        // Walk the hierarchy to find MediaSessionService
        val hierarchy = generateSequence(superclass) { it.superclass }
        val mediaSessionServiceFound = hierarchy.any {
            it.simpleName == "MediaSessionService"
        }
        assert(mediaSessionServiceFound) {
            "PlaybackService must extend MediaSessionService"
        }
    }

    @Test
    fun `PlaybackService companion object is accessible`() {
        val companion = PlaybackService.Companion
        assertNotNull("Companion object should be accessible", companion)
    }

    // -------------------------------------------------------------------------
    // Command value format
    // -------------------------------------------------------------------------

    @Test
    fun `skip silence command uses UPPER_SNAKE_CASE format`() {
        val command = PlaybackService.CUSTOM_COMMAND_SET_SKIP_SILENCE
        assert(command.matches(Regex("^[A-Z_]+$"))) {
            "CUSTOM_COMMAND_SET_SKIP_SILENCE should be UPPER_SNAKE_CASE, was: $command"
        }
    }

    @Test
    fun `volume boost command uses UPPER_SNAKE_CASE format`() {
        val command = PlaybackService.CUSTOM_COMMAND_SET_VOLUME_BOOST
        assert(command.matches(Regex("^[A-Z_]+$"))) {
            "CUSTOM_COMMAND_SET_VOLUME_BOOST should be UPPER_SNAKE_CASE, was: $command"
        }
    }
}
