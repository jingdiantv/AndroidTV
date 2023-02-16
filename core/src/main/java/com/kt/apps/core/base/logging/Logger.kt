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
            Log.d(logTag, message)
        }
    }

}
