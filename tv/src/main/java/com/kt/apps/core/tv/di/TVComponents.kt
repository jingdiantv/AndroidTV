package com.kt.apps.core.tv.di

import com.kt.apps.core.di.CoreComponents
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.usecase.GetChannelLinkStreamById
import com.kt.apps.core.tv.usecase.GetListTVChannel
import com.kt.apps.core.tv.usecase.GetTVChannelLinkStreamFrom
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import dagger.Component
import javax.inject.Named

@Component(
    modules = [TVChannelModule::class],
    dependencies = [CoreComponents::class]
)
@TVScope
interface TVComponents {

    fun providesInteractors(): TVChannelInteractors
    fun providesTVDataSourceMap(): Map<TVDataSourceFrom, @JvmSuppressWildcards ITVDataSource>
    fun getChannelLinkStreamFrom(): GetTVChannelLinkStreamFrom
    fun getListTVChannel(): GetListTVChannel
    fun providesGetChannelLinkStreamById(): GetChannelLinkStreamById
}