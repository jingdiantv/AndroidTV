package com.kt.apps.core.di

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.logging.ActionLoggerFactory
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.local.RoomDataBase
import dagger.BindsInstance
import dagger.Component
import io.reactivex.rxjava3.disposables.DisposableContainer
import okhttp3.OkHttpClient

@Component(
    modules = [
        CoreModule::class,
        StorageModule::class,
        FirebaseModule::class,
        NetworkModule::class,
    ]
)
@CoreScope
interface CoreComponents {
    fun disposableContainer(): DisposableContainer
    fun roomDatabase(): RoomDataBase
    fun firebaseRemoteConfig(): FirebaseRemoteConfig
    fun firebaseDatabase(): FirebaseDatabase
    fun firebaseAnalytics(): FirebaseAnalytics
    fun sharedPreferences(): SharedPreferences
    fun okHttpClient(): OkHttpClient
    fun coreApp(): CoreApp
    fun context(): Context

    fun exoPlayerManager(): ExoPlayerManagerMobile

    fun parserExtensionsSource(): ParserExtensionsSource

    fun inject(scope: BaseViewModel)

    fun keyValueStorage(): IKeyValueStorage

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(context: CoreApp): Builder

        @BindsInstance
        fun context(context: Context): Builder
        fun storageModule(storageModule: StorageModule): Builder
        fun build(): CoreComponents
    }
}