package com.kt.apps.media.mobile.di.main

import com.kt.apps.media.mobile.ui.main.MainActivity
import com.kt.apps.media.mobile.di.viewmodels.ViewModelModule
import com.kt.apps.media.mobile.ui.playback.PlaybackActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainTVModule {

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playback(): PlaybackActivity


}