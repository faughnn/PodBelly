package com.podbelly.core.common.di

import javax.inject.Qualifier

/** Qualifies the IO [kotlinx.coroutines.CoroutineDispatcher] so it can be injected and overridden in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
