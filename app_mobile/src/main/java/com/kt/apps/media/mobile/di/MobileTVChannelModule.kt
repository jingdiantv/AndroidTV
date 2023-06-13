package com.kt.apps.media.mobile.di

import com.kt.apps.core.tv.di.TVChannelModule
import com.kt.apps.core.tv.di.TVScope
import dagger.Provides

class MobileTVChannelModule: TVChannelModule() {
    override fun providesTimeout(): Long? = 5
}