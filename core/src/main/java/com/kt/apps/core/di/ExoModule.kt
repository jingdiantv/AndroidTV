package com.kt.apps.core.di

import com.kt.apps.core.repository.IMediaHistoryRepository
import com.kt.apps.core.repository.MediaHistoryRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
abstract class ExoModule {
    @Binds
    abstract fun bindHistoryRepo(repoImpl: MediaHistoryRepositoryImpl): IMediaHistoryRepository
}