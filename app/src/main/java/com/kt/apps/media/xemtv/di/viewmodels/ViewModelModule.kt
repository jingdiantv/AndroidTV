package com.kt.apps.media.xemtv.di.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseViewModelFactory
import com.kt.apps.core.di.CoreScope
import com.kt.apps.media.xemtv.di.AppScope
import dagger.Binds
import dagger.Module

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindViewModelFactory(
        factory: BaseViewModelFactory
    ): ViewModelProvider.Factory
}