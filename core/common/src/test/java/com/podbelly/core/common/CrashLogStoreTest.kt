package com.podbelly.core.common

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CrashLogStoreTest {

    private lateinit var tempDir: File
    private lateinit var context: Context
    private lateinit var store: CrashLogStore

    @Before
    fun setUp() {
        // kotlin.io.createTempDir is deprecated-as-error from Kotlin 2.3.
        tempDir = createTempDirectory(prefix = "crash-log-test").toFile()
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempDir
        store = CrashLogStore(context)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `read returns null when no logs exist`() {
        assertNull(store.read())
        assertFalse(store.hasLogs())
    }

    @Test
    fun `append writes a stack trace that can be read back`() {
        val ex = RuntimeException("boom")
        store.append(ex, threadName = "main", appVersion = "1.0.0")

        val content = store.read()
        assertNotNull(content)
        assertTrue(content!!.contains("boom"))
        assertTrue(content.contains("App version: 1.0.0"))
        assertTrue(content.contains("Thread: main"))
        assertTrue(store.hasLogs())
    }

    @Test
    fun `multiple appends accumulate in the file`() {
        store.append(RuntimeException("first"), "main", "1.0.0")
        store.append(IllegalStateException("second"), "worker-1", "1.0.0")

        val content = store.read()!!
        assertTrue(content.contains("first"))
        assertTrue(content.contains("second"))
        assertTrue(content.contains("Thread: worker-1"))
    }

    @Test
    fun `clear removes the log file`() {
        store.append(RuntimeException("boom"), "main", "1.0.0")
        assertTrue(store.hasLogs())

        store.clear()

        assertFalse(store.hasLogs())
        assertNull(store.read())
    }

    @Test
    fun `file is rotated when exceeding max size`() {
        // Pre-fill the file just past the cap so the next append must rotate.
        val file = File(tempDir, CrashLogStore.FILE_NAME)
        file.writeText("x".repeat((CrashLogStore.MAX_BYTES + 100).toInt()))

        store.append(RuntimeException("fresh-entry"), "main", "1.0.0")

        val content = store.read()!!
        // The pre-filled padding should be gone; only the new entry remains.
        assertTrue(content.contains("fresh-entry"))
        assertFalse(content.contains("xxxxxxxxxx"))
        assertTrue(content.length < CrashLogStore.MAX_BYTES)
    }
}
