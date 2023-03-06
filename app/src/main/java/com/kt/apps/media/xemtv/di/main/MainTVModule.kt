package com.kt.apps.media.xemtv.di.main

import androidx.lifecycle.ViewModel
import com.kt.apps.media.xemtv.ui.details.DetailsActivity
import com.kt.apps.media.xemtv.ui.main.MainActivity
import com.kt.apps.media.xemtv.ui.main.MainFragment
import com.kt.apps.media.xemtv.ui.details.VideoDetailsFragment
import com.kt.apps.media.xemtv.di.viewmodels.ViewModelKey
import com.kt.apps.media.xemtv.di.viewmodels.ViewModelModule
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.main.DashboardFragment
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import com.kt.apps.media.xemtv.ui.playback.PlaybackVideoFragment
import com.kt.apps.media.xemtv.ui.tv.FragmentTVDashboard
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class MainTVModule {

    @Binds
    @IntoMap
    @ViewModelKey(TVChannelViewModel::class)
    abstract fun bindTVChannelViewModel(tvChannelViewModel: TVChannelViewModel): ViewModel

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun mainFragment(): MainFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun detailActivity(): DetailsActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun detailFragment(): VideoDetailsFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playbackActivity(): PlaybackActivity

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun playbackFragment(): PlaybackVideoFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun dashboardFragment(): DashboardFragment

    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    internal abstract fun tvDashboardFragment(): FragmentTVDashboard


}