package com.kt.apps.media.xemtv

import android.app.Activity
import android.os.Bundle
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.di.DaggerCoreComponents
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.di.DaggerTVComponents
import com.kt.apps.core.tv.di.TVComponents
import com.kt.apps.football.di.DaggerFootballComponents
import com.kt.apps.football.di.FootballComponents
import com.kt.apps.media.xemtv.di.AppComponents
import com.kt.apps.media.xemtv.di.DaggerAppComponents
import com.kt.apps.media.xemtv.workers.TVRecommendationWorkers
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import javax.inject.Inject

class App : CoreApp() {

    private val _coreComponents by lazy {
        DaggerCoreComponents.builder()
            .application(this)
            .context(this)
            .build()
    }

    private val _tvComponents by lazy {
        DaggerTVComponents.builder()
            .coreComponents(_coreComponents)
            .build()
    }

    private val _footballComponent by lazy {
        DaggerFootballComponents.builder()
            .coreComponents(_coreComponents)
            .build()
    }

    val coreComponents: CoreComponents
        get() = _coreComponents

    val tvComponents: TVComponents
        get() = _tvComponents

    val footballComponents: FootballComponents
        get() = _footballComponent

    @Inject
    lateinit var workManager: WorkManager

    override fun onCreate() {
        super.onCreate()
        app = this
        Firebase.initialize(this)
        (applicationInjector() as AppComponents).inject(this)
        Firebase.remoteConfig.fetch(20)
        Firebase.remoteConfig
            .fetchAndActivate()
            .addOnSuccessListener {
                Logger.d(this, tag = "RemoteConfig", message = "Success")
            }
            .addOnFailureListener {
                Logger.d(this, tag = "RemoteConfig", message = "Failure")
            }
        workManager.enqueue(
            OneTimeWorkRequestBuilder<TVRecommendationWorkers>()
                .setInputData(Data.Builder()
                    .putInt(TVRecommendationWorkers.EXTRA_TYPE, TVRecommendationWorkers.Type.ALL.value)
                    .build())
                .build()
        )
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponents.builder()
            .tvComponents(_tvComponents)
            .coreComponents(_coreComponents)
            .footballComponent(_footballComponent)
            .app(this)
            .build()
    }

    companion object {
        private lateinit var app: App
        fun get() = app
    }


}