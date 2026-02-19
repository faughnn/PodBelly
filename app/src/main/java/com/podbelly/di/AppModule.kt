package com.podbelly.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * App-level Hilt module.
 *
 * DataStore<Preferences> is already provided by [com.podbelly.core.common.di.CommonModule].
 * This module exists as a place to add any additional app-level bindings that are not
 * covered by the core or feature modules.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
