package com.kt.apps.media.mobile.logger

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.kt.apps.core.logging.IActionLogger
import javax.inject.Inject

class MobileActionLoggerImpl @Inject constructor(
    private val analytics: FirebaseAnalytics
) : IActionLogger {
    override fun log(event: String, extras: Bundle) {
        analytics.logEvent("${event}_Mobile", extras)
    }
}