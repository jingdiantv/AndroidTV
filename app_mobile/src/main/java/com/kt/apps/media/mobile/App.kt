package com.kt.apps.media.mobile

import androidx.work.Configuration
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
import com.kt.apps.media.mobile.di.AppComponents
import com.kt.apps.media.mobile.di.DaggerAppComponents
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import javax.inject.Inject

class App : CoreApp(), Configuration.Provider {

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

    val appComponents: AppComponents
        get() = applicationInjector() as AppComponents

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
        Firebase.remoteConfig.fetch(20)
        Firebase.remoteConfig
            .fetchAndActivate()
            .addOnSuccessListener {
                Logger.d(this, tag = "RemoteConfig", message = "Success")
            }
            .addOnFailureListener {
                Logger.d(this, tag = "RemoteConfig", message = "Failure")
            }
        (applicationInjector() as AppComponents).inject(this)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponents.builder()
            .app(this)
            .coreComponents(_coreComponents)
            .tvComponents(_tvComponents)
            .footballComponent(_footballComponent)
            .build()
    }

    companion object {
        private lateinit var app: App
        fun get() = app
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setDefaultProcessName("com.kt.apps")
            .build()
    }


}