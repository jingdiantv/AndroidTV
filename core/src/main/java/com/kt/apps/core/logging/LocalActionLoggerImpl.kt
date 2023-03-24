package com.kt.apps.core.logging

import android.os.Bundle
import com.kt.apps.core.di.CoreScope
import javax.inject.Inject

class LocalActionLoggerImpl @Inject constructor() : IActionLogger {
    override fun log(event: String, extras: Bundle) {
        Logger.d(this, event, extras.toString())
    }

    init {
        Logger.d(this, message = "create logger: $instance")
        instance++
    }

    companion object {
        private var instance = 0
    }
}