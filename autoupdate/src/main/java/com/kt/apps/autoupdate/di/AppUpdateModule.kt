package com.kt.apps.autoupdate.di

import com.kt.apps.autoupdate.IUpdateRepository
import com.kt.apps.autoupdate.UpdateRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
abstract class AppUpdateModule {

    @Binds
    abstract fun provideRepository(
        impl: UpdateRepositoryImpl
    ): IUpdateRepository
}