package com.kt.apps.core.di

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
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
        NetworkModule::class
    ]
)
@CoreScope
interface CoreComponents {
    fun disposableContainer(): DisposableContainer
    fun roomDatabase(): RoomDataBase
    fun firebaseRemoteConfig(): FirebaseRemoteConfig
    fun firebaseDatabase(): FirebaseDatabase
    fun sharedPreferences(): SharedPreferences
    fun okHttpClient(): OkHttpClient

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(context: Context): Builder
        fun storageModule(storageModule: StorageModule): Builder
        fun build(): CoreComponents
    }
}