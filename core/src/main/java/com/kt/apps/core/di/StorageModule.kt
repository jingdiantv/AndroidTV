package com.kt.apps.core.di

import android.content.Context
import android.content.SharedPreferences
import com.kt.apps.core.Constants
import com.kt.apps.core.storage.local.RoomDataBase
import dagger.Module
import dagger.Provides
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.DisposableContainer
import javax.inject.Named

@Module
class StorageModule {

    @Provides
    @CoreScope
    fun providesDisposable(): DisposableContainer = CompositeDisposable()

    @Provides
    @CoreScope
    @Named(EXTRA_EXO_DISPOSABLE)
    fun provideNetworkDisposable(): DisposableContainer = CompositeDisposable()


    @Provides
    @CoreScope
    @Named(Constants.SHARE_PREF_NAME)
    fun providesSharePreferenceName(): String = "XemTV"

    @Provides
    @CoreScope
    fun providesDefaultSharePreference(
        context: Context,
        @Named(Constants.SHARE_PREF_NAME)
        sharedPreferencesName: String
    ): SharedPreferences {
        return context.getSharedPreferences(
            sharedPreferencesName,
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @CoreScope
    fun providesRoomDatabase(
        context:
        Context
    ): RoomDataBase {
        return RoomDataBase.getInstance(context)
    }

    companion object {
        const val EXTRA_EXO_DISPOSABLE = "extra:exo_disposable"
    }


}