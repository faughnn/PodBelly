package com.podbelly.core.common

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores uncaught-exception stack traces in a single rolling file inside the
 * app's private storage so the user can share them later from Settings.
 *
 * The file is capped: once it exceeds [MAX_BYTES], the next append rotates it
 * (the oldest entries are dropped). All public methods are safe to call from
 * any thread, including the crashing thread inside an uncaught-exception
 * handler.
 */
@Singleton
class CrashLogStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val lock = Any()

    private val file: File
        get() = File(context.filesDir, FILE_NAME)

    fun append(throwable: Throwable, threadName: String, appVersion: String) {
        val entry = buildString {
            append("===== ")
            append(timestampFormat.format(Instant.now()))
            append(" =====\n")
            append("App version: ").append(appVersion).append('\n')
            append("Android: ").append(android.os.Build.VERSION.RELEASE)
                .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n")
            append("Device: ").append(android.os.Build.MANUFACTURER).append(' ')
                .append(android.os.Build.MODEL).append('\n')
            append("Thread: ").append(threadName).append('\n')
            append(stackTraceOf(throwable))
            append('\n')
        }

        // Measure in UTF-8 bytes (what appendText writes), not String char count, so
        // multi-byte device/exception text doesn't blow past MAX_BYTES unnoticed.
        val entryBytes = entry.toByteArray()

        synchronized(lock) {
            try {
                val f = file
                if (f.exists() && f.length() + entryBytes.size > MAX_BYTES) {
                    // Rotate by keeping the most recent entries instead of discarding the
                    // whole log — a crash near the cap shouldn't wipe all prior traces.
                    // Trim by BYTES, not chars — the cap (MAX_BYTES) is measured in bytes, so
                    // retaining a fixed char count could keep far more than KEEP_BYTES of
                    // multi-byte UTF-8 and overshoot the cap.
                    val existingBytes = f.readBytes()
                    val keepFrom = (existingBytes.size - KEEP_BYTES).coerceAtLeast(0)
                    // Decoding from an arbitrary byte offset may split a multi-byte char, but
                    // aligning to the next entry boundary below discards any leading partial.
                    val tail = String(existingBytes, keepFrom, existingBytes.size - keepFrom, Charsets.UTF_8)
                    // Align to an entry boundary so we don't keep a half stack trace.
                    val boundary = tail.indexOf(ENTRY_MARKER)
                    val kept = if (boundary >= 0) tail.substring(boundary) else ""
                    f.writeText(kept)
                }
                f.appendText(entry)
            } catch (_: Throwable) {
                // Never let crash logging itself throw — the process is dying anyway.
            }
        }
    }

    fun read(): String? = synchronized(lock) {
        val f = file
        if (!f.exists() || f.length() == 0L) null else f.readText()
    }

    fun hasLogs(): Boolean = synchronized(lock) {
        val f = file
        f.exists() && f.length() > 0L
    }

    fun clear() {
        synchronized(lock) {
            file.delete()
        }
    }

    private fun stackTraceOf(throwable: Throwable): String {
        val sw = StringWriter()
        PrintWriter(sw).use { throwable.printStackTrace(it) }
        return sw.toString()
    }

    companion object {
        const val FILE_NAME = "crash_logs.txt"
        const val MAX_BYTES = 200 * 1024L

        /** Marks the start of each log entry; used to trim on an entry boundary. */
        private const val ENTRY_MARKER = "===== "

        /** Roughly how many bytes of the log to retain when rotating (about half the cap). */
        private const val KEEP_BYTES = 100 * 1024

        // DateTimeFormatter is immutable and thread-safe (unlike SimpleDateFormat), so it
        // is safe to format outside the lock even when two threads crash concurrently.
        private val timestampFormat = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
            .withZone(ZoneId.systemDefault())
    }
}
