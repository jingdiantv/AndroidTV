package com.kt.apps.media.xemtv.di

import com.kt.apps.core.base.player.ExoPlayerManager
import com.kt.apps.core.di.CoreComponents
import com.kt.apps.media.xemtv.di.viewmodels.ViewModelModule
import com.kt.apps.core.tv.di.TVComponents
import com.kt.apps.football.datasource.IFootballMatchDataSource
import com.kt.apps.football.di.FootballComponents
import com.kt.apps.football.model.FootballDataSourceFrom
import com.kt.apps.media.xemtv.App
import com.kt.apps.media.xemtv.di.main.MainTVModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule

@Component(
    dependencies = [CoreComponents::class,
        TVComponents::class,
        FootballComponents::class],
    modules = [
        ViewModelModule::class,
        AndroidSupportInjectionModule::class,
        MainTVModule::class
    ]
)
@AppScope
interface AppComponents : AndroidInjector<App> {

    fun exoPlayerManager(): ExoPlayerManager

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun app(app: App): Builder
        fun coreComponents(coreComponents: CoreComponents): Builder
        fun tvComponents(tvComponents: TVComponents): Builder
        fun footballComponent(footballComponents: FootballComponents): Builder
        fun build(): AppComponents
    }

}