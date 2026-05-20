package com.podbelly.core.common

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
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
            append(timestampFormat.format(Date()))
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

        synchronized(lock) {
            try {
                val f = file
                if (f.exists() && f.length() + entry.length > MAX_BYTES) {
                    f.delete()
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

        private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
}
