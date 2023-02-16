package com.kt.apps.media.xemtv

import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.di.DaggerCoreComponents
import com.kt.apps.core.tv.di.DaggerTVComponents
import com.kt.apps.core.tv.di.TVComponents
import com.kt.apps.media.xemtv.di.DaggerAppComponents
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication

class App : DaggerApplication() {

    private val _coreComponents by lazy {
        DaggerCoreComponents.builder()
            .application(this)
            .build()
    }

    private val _tvComponents by lazy {
        DaggerTVComponents.builder()
            .coreComponents(_coreComponents)
            .build()
    }

    val coreComponents: CoreComponents
        get() = _coreComponents

    val tvComponents: TVComponents
        get() = _tvComponents

    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponents.builder()
            .tvComponents(_tvComponents)
            .coreComponents(_coreComponents)
            .app(this)
            .build()
    }


}