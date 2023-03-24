package com.kt.apps.core.di

import com.kt.apps.core.logging.FirebaseActionLoggerImpl
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.LocalActionLoggerImpl
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass


@MapKey
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class LoggerKey(val value: KClass<out IActionLogger>)

@Module
abstract class CoreLoggerModule {
    @Binds
    @IntoMap
    @LoggerKey(LocalActionLoggerImpl::class)
    abstract fun providesLocalActionLog(localActionLog: LocalActionLoggerImpl): IActionLogger

    @Binds
    @IntoMap
    @LoggerKey(FirebaseActionLoggerImpl::class)
    abstract fun providesActionLog(localActionLog: FirebaseActionLoggerImpl): IActionLogger

}