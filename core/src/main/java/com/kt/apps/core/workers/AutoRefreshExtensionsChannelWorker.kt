package com.kt.apps.core.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.storage.getLastRefreshExtensions
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.saveLastRefreshExtensions
import io.reactivex.rxjava3.disposables.CompositeDisposable

class AutoRefreshExtensionsChannelWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    private val roomDatabase by lazy {
        RoomDataBase.getInstance(context)
    }

    private val extensionsDao by lazy {
        roomDatabase.extensionsConfig()
    }

    private val extensionsChannelDao by lazy {
        roomDatabase.extensionsChannelDao()
    }


    private val keyValueStorage by lazy {
        (context.applicationContext as CoreApp)
            .coreComponents
            .keyValueStorage()
    }

    private val parserExtensionsSource by lazy {
        (context.applicationContext as CoreApp)
            .coreComponents
            .parserExtensionsSource()
    }

    init {
        Log.e("TAG", "init work")
    }

    override fun doWork(): Result {
        Log.e("TAG", "doWork")
        CompositeDisposable()
            .add(
                extensionsDao.getAll()
                    .flatMapIterable {
                        it
                    }
                    .filter {
                        System.currentTimeMillis() - keyValueStorage.getLastRefreshExtensions(it) > 0.5 * HOUR
                    }
                    .flatMap {
                        parserExtensionsSource.parseFromRemoteRx(it)
                            .doOnComplete {
                                keyValueStorage.saveLastRefreshExtensions(it)
                            }
                    }
                    .subscribe({
                        Log.e("TAG", "Refresh success")
                    }, {
                        Log.e("TAG", "Refresh error: ${it.message}", it)
                    }, {
                        Log.e("TAG", "Refresh")
                    })

            )
        return Result.success()
    }

    companion object {
        private const val HOUR = 60 * 60 * 1000L
    }

}