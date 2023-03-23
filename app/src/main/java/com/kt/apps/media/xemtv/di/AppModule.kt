package com.kt.apps.media.xemtv.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides

@Module
class AppModule {

    @Provides
    @AppScope
    fun provideWorkerManager(context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}