package com.kt.apps.football.di

import com.kt.apps.core.di.CoreComponents
import com.kt.apps.football.datasource.IFootballMatchDataSource
import com.kt.apps.football.di.scope.FootballScope
import com.kt.apps.football.model.FootballDataSourceFrom
import com.kt.apps.football.usecase.GetLinkStreamForFootballMatch
import com.kt.apps.football.usecase.GetListFootballMatch
import dagger.Component

@Component(modules = [FootballModule::class], dependencies = [CoreComponents::class])
@FootballScope
interface FootballComponents {
    fun providesTVDataSourceMap(): Map<FootballDataSourceFrom, @JvmSuppressWildcards IFootballMatchDataSource>
    fun getLinkStreamForFootballMatch(): GetLinkStreamForFootballMatch
    fun getListFootballMatch(): GetListFootballMatch
}