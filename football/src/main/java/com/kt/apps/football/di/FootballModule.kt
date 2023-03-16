package com.kt.apps.football.di

import com.kt.apps.football.datasource.IFootballMatchDataSource
import com.kt.apps.football.datasource.footballmatches.Football91DataSource
import com.kt.apps.football.di.scope.FootballDataSourceMapKey
import com.kt.apps.football.di.scope.FootballScope
import com.kt.apps.football.di.scope.Source91PhutConfig
import com.kt.apps.football.model.FootballDataSourceFrom
import com.kt.apps.football.model.FootballRepositoryConfig
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
class FootballModule {

    @Provides
    @Source91PhutConfig
    @FootballScope
    fun providesConfig(): FootballRepositoryConfig = FootballRepositoryConfig(
        url = "https://90ptv.vip/",
        regex = "(?<=urlStream\\s=\\s\").*?(?=\")"
    )

    @Provides
    @IntoMap
    @FootballDataSourceMapKey(FootballDataSourceFrom.Phut91)
    @FootballScope
    fun providesFootball91DataSource(
        impl: Football91DataSource
    ): IFootballMatchDataSource = impl

}