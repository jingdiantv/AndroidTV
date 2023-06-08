package com.kt.apps.media.xemtv.di.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseViewModelFactory
import com.kt.apps.core.di.CoreScope
import com.kt.apps.media.xemtv.di.AppScope
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.extensions.ExtensionsViewModel
import com.kt.apps.media.xemtv.ui.football.FootballViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(TVChannelViewModel::class)
    abstract fun bindTVChannelViewModel(tvChannelViewModel: TVChannelViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FootballViewModel::class)
    abstract fun bindFootballViewModel(footballViewModel: FootballViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ExtensionsViewModel::class)
    abstract fun bindExtensionsViewModel(extensionsViewModel: ExtensionsViewModel): ViewModel


    @Binds
    abstract fun bindViewModelFactory(
        factory: BaseViewModelFactory
    ): ViewModelProvider.Factory
}