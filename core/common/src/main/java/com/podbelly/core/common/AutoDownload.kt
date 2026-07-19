package com.podbelly.core.common

import com.podbelly.core.database.entity.AUTO_DOWNLOAD_ALWAYS
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_NEVER

/**
 * Whether a show's newly arrived episodes should be auto-downloaded.
 *
 * The per-show override wins: Always downloads even when the global smart
 * setting is off (it's an explicit choice made on the podcast page), Never
 * blocks a show regardless of listening activity. Shows on the default Smart
 * mode follow the global switch plus recent engagement. Network/charging
 * gates are the caller's job — they apply to every mode.
 */
fun shouldAutoDownload(autoDownloadMode: Int, smartEnabled: Boolean, engaged: Boolean): Boolean =
    when (autoDownloadMode) {
        AUTO_DOWNLOAD_ALWAYS -> true
        AUTO_DOWNLOAD_NEVER -> false
        else -> smartEnabled && engaged
    }

/** A just-inserted episode row that is a candidate for auto-download. */
data class AutoDownloadCandidate(
    val episodeId: Long,
    val publicationDate: Long,
)
