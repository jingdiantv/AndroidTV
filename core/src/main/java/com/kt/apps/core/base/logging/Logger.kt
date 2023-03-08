package com.kt.apps.core.base.logging

import android.util.Log
import com.kt.apps.core.BuildConfig

object Logger {
    fun <T : Any> e(t: T, tag: String? = null, message: String) {
        if (BuildConfig.DEBUG) {
            var logTag = t::class.java.simpleName
            if (!tag?.trim().isNullOrEmpty()) {
                logTag += "_$tag"
            }
            Log.e(logTag, message)
        }
    }

    fun <T : Any> e(t: T, tag: String? = null, exception: Throwable) {
        if (BuildConfig.DEBUG) {
            var logTag = t::class.java.simpleName
            if (!tag?.trim().isNullOrEmpty()) {
                logTag += "_$tag"
            }
            Log.e(logTag, exception.message, exception)
        }
    }

    fun <T : Any> d(t: T, tag: String? = null, message: String) {
        if (BuildConfig.DEBUG) {
            var logTag = t::class.java.simpleName
            if (!tag?.trim().isNullOrEmpty()) {
                logTag += "_$tag"
            }
            val minMsgLength = MIN_MESSAGE_LENGTH
            if (message.length < minMsgLength) {
                Log.d(logTag, message)
            } else {
                var index = 0
                var subStr: String
                var range = 0

                while (message.length > index + 1) {
                    range = if (message.length - index > minMsgLength) minMsgLength else {
                        message.length - index
                    }
                    subStr = message.substring(index, index + range)
                    index += range
                    Log.d(logTag, subStr)
                }
            }
        }
    }

    private const val MIN_MESSAGE_LENGTH = 4000

}
