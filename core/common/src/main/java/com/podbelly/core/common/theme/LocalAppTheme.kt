package com.podbelly.core.common.theme

import androidx.compose.runtime.compositionLocalOf
import com.podbelly.core.common.AppTheme

/**
 * The user's currently selected [AppTheme], provided by `PodbellTheme` so any
 * composable can name the active theme without plumbing it through view models.
 * Used by the share card to label which theme a shared image was made with.
 */
val LocalAppTheme = compositionLocalOf { AppTheme.SYSTEM }
