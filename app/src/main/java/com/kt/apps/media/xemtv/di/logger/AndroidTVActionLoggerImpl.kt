package com.kt.apps.media.xemtv.di.logger

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import javax.inject.Inject

class AndroidTVActionLoggerImpl @Inject constructor(
    private val analytics: FirebaseAnalytics
) : IActionLogger {
    override fun log(event: String, extras: Bundle) {
        Logger.d(this, event, "$extras")
        analytics.logEvent("${event}_AndroidTV", extras)
    }
}