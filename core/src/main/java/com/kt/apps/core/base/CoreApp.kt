package com.kt.apps.core.base

import dagger.android.AndroidInjector
import dagger.android.DaggerApplication

abstract class CoreApp : DaggerApplication() {

    override fun onCreate() {
        super.onCreate()
        app = this
    }

    companion object {
        private lateinit var app: CoreApp
        fun getInstance(): CoreApp {
            return app
        }
    }
}