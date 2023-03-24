package com.kt.apps.core.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Module
import dagger.Provides

@Module
class FirebaseModule {

    @Provides
    @CoreScope
    fun providesFirebaseDataBase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @CoreScope
    fun providesRemoteConfig(): FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    @Provides
    @CoreScope
    fun providesAnalytics(): FirebaseAnalytics = Firebase.analytics

}