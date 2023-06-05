package com.kt.apps.core.base

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.kt.apps.core.Constants
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.logging.Logger
import dagger.android.DaggerApplication

abstract class CoreApp : DaggerApplication(), ActivityLifecycleCallbacks {
    abstract val coreComponents: CoreComponents

    override fun onCreate() {
        super.onCreate()
        app = this
        Firebase.initialize(this)
        Firebase.remoteConfig
            .setDefaultsAsync(mapOf(
                Constants.EXTRA_KEY_USE_ONLINE to true,
                Constants.EXTRA_KEY_VERSION_NEED_REFRESH to 1L
            ))
        Firebase.remoteConfig
            .fetchAndActivate()
            .addOnSuccessListener {
                Logger.d(this, tag = "RemoteConfig", message = "Success")
            }
            .addOnFailureListener {
                Logger.d(this, tag = "RemoteConfig", message = "Failure")
            }
        Firebase.remoteConfig.fetch(20)
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