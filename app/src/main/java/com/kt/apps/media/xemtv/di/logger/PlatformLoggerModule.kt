package com.kt.apps.media.xemtv.di.logger

import com.kt.apps.core.di.CoreLoggerModule
import com.kt.apps.core.di.LoggerKey
import com.kt.apps.core.logging.IActionLogger
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class PlatformLoggerModule : CoreLoggerModule() {
    @Binds
    @IntoMap
    @LoggerKey(AndroidTVActionLoggerImpl::class)
    abstract fun bindActionLogger(instance: AndroidTVActionLoggerImpl): IActionLogger
}