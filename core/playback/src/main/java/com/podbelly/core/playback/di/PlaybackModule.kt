package com.podbelly.core.playback.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the playback layer.
 *
 * [com.podbelly.core.playback.PlaybackController] and
 * [com.podbelly.core.playback.SleepTimer] are provided automatically via
 * `@Singleton @Inject constructor` -- no explicit `@Provides` methods are needed.
 *
 * The [com.podbelly.core.database.dao.EpisodeDao] and
 * [com.podbelly.core.database.dao.QueueDao] dependencies come from the
 * `:core:database` module's own Hilt module.
 *
 * This module exists as a placeholder for any future bindings that may be
 * required by the playback layer (e.g., interface-to-implementation bindings).
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule
