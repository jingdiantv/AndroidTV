package com.kt.apps.media.xemtv.di.workers

import androidx.work.ListenableWorker
import dagger.MapKey
import kotlin.reflect.KClass

@MapKey
@MustBeDocumented
@Retention()
annotation class WorkerKey(
    val value: KClass<out ListenableWorker>
)

class WorkerModule {
}
