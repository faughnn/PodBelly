package com.podbelly.core.common

interface CrashReporter {
    fun logMessage(message: String)
    fun recordException(throwable: Throwable, keys: Map<String, String> = emptyMap())
}
