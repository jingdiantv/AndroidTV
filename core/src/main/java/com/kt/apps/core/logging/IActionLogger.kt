package com.kt.apps.core.logging

import android.os.Bundle

interface IActionLogger {
    fun log(event: String, extras: Bundle)
}