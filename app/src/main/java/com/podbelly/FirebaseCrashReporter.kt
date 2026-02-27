package com.podbelly

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.podbelly.core.common.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    override fun logMessage(message: String) {
        crashlytics.log(message)
    }

    override fun recordException(throwable: Throwable, keys: Map<String, String>) {
        val instance = crashlytics
        keys.forEach { (key, value) -> instance.setCustomKey(key, value) }
        instance.recordException(throwable)
    }
}
