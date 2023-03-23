package com.kt.apps.media.xemtv.di.main

import androidx.lifecycle.ViewModel
import com.kt.apps.media.xemtv.ui.details.DetailsActivity
import com.kt.apps.media.xemtv.ui.main.MainActivity
import com.kt.apps.media.xemtv.ui.details.VideoDetailsFragment
import com.kt.apps.media.xemtv.di.viewmodels.ViewModelKey
import com.kt.apps.media.xemtv.di.viewmodels.ViewModelModule
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.football.FootballFragment
import com.kt.apps.media.xemtv.ui.football.FootballPlaybackFragment
import com.kt.apps.media.xemtv.ui.football.FootballViewModel
import com.kt.apps.media.xemtv.ui.main.DashboardFragment
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import com.kt.apps.media.xemtv.ui.playback.TVPlaybackVideoFragment
import com.kt.apps.media.xemtv.ui.radio.RadioFragment
import com.kt.apps.media.xemtv.ui.tv.FragmentTVDashboard
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class MainTVModule {

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun detailActivity(): DetailsActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun detailFragment(): VideoDetailsFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playbackActivity(): PlaybackActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playbackFragment(): TVPlaybackVideoFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun dashboardFragment(): DashboardFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun tvDashboardFragment(): FragmentTVDashboard

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun footballListFragment(): FootballFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun footballPlaybackFragment(): FootballPlaybackFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun radioFragment(): RadioFragment


}