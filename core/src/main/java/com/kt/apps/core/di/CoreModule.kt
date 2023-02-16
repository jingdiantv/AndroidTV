package com.kt.apps.core.di

import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.KeyValueStorageImpl
import dagger.Binds
import dagger.Module

@Module
abstract class CoreModule {

    @Binds
    @CoreScope
    abstract fun bindsKeyValueStorage(
        keyValueStorageImpl: KeyValueStorageImpl
    ): IKeyValueStorage

    companion object {
    }
}