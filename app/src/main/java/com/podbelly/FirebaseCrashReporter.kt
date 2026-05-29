package com.podbelly

import com.google.firebase.crashlytics.CustomKeysAndValues
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
        // Attach keys to *this* report only. setCustomKey() sets process-global keys
        // that would otherwise leak into every later report (and any fatal crash).
        val builder = CustomKeysAndValues.Builder()
        keys.forEach { (key, value) -> builder.putString(key, value) }
        crashlytics.recordException(throwable, builder.build())
    }
}
