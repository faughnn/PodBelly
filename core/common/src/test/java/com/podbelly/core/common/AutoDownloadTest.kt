package com.podbelly.core.common

import com.podbelly.core.database.entity.AUTO_DOWNLOAD_ALWAYS
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_NEVER
import com.podbelly.core.database.entity.AUTO_DOWNLOAD_SMART
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoDownloadTest {

    @Test
    fun `smart mode follows the global switch and engagement`() {
        assertTrue(shouldAutoDownload(AUTO_DOWNLOAD_SMART, smartEnabled = true, engaged = true))
        assertFalse(shouldAutoDownload(AUTO_DOWNLOAD_SMART, smartEnabled = true, engaged = false))
        assertFalse(shouldAutoDownload(AUTO_DOWNLOAD_SMART, smartEnabled = false, engaged = true))
        assertFalse(shouldAutoDownload(AUTO_DOWNLOAD_SMART, smartEnabled = false, engaged = false))
    }

    @Test
    fun `always overrides both the switch and engagement`() {
        assertTrue(shouldAutoDownload(AUTO_DOWNLOAD_ALWAYS, smartEnabled = false, engaged = false))
        assertTrue(shouldAutoDownload(AUTO_DOWNLOAD_ALWAYS, smartEnabled = true, engaged = true))
    }

    @Test
    fun `never blocks even an engaged show`() {
        assertFalse(shouldAutoDownload(AUTO_DOWNLOAD_NEVER, smartEnabled = true, engaged = true))
        assertFalse(shouldAutoDownload(AUTO_DOWNLOAD_NEVER, smartEnabled = false, engaged = false))
    }
}
