package com.kt.apps.core.base

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication

abstract class CoreApp : DaggerApplication(), ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        app = this
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityCount--
    }


    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        activityCount++
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }



    companion object {
        var activityCount = 0
        private lateinit var app: CoreApp
        fun getInstance(): CoreApp {
            return app
        }
    }
}