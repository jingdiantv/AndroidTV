package com.kt.apps.media.mobile.di.workers

import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import dagger.Binds
import dagger.MapKey
import dagger.Module
import kotlin.reflect.KClass

@MapKey
@MustBeDocumented
@Retention()
annotation class WorkerKey(
    val value: KClass<out ListenableWorker>
)

@Module
abstract class WorkerModule {
}
