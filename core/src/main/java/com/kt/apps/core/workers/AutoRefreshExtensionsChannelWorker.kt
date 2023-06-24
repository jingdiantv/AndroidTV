package com.kt.apps.core.workers

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.getLastRefreshExtensions
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.saveLastRefreshExtensions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class AutoRefreshExtensionsChannelWorker(
    context: Context,
    params: WorkerParameters
) : RxWorker(context, params) {
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
        Logger.d(this@AutoRefreshExtensionsChannelWorker, message = "init work")
    }

    override fun createWork(): Single<Result> {
        val isBetaVersion = inputData.getBoolean(EXTRA_KEY_VERSION_IS_BETA, false)
        val autoRefreshSource = extensionsDao.getAll()
            .flattenAsObservable {
                it
            }.filter {
                System.currentTimeMillis() - keyValueStorage.getLastRefreshExtensions(it) >= parserExtensionsSource.getIntervalRefreshData(
                    it.type
                )
            }.concatMapCompletable { extensionsConfig ->
                if (extensionsConfig.type == ExtensionsConfig.Type.FOOTBALL) {
                    roomDatabase.extensionsChannelDao()
                        .deleteBySourceId(extensionsConfig.sourceUrl)
                        .andThen(
                            parseExtensionsSource(extensionsConfig)
                        )
                } else {
                    parseExtensionsSource(extensionsConfig)
                }
            }
            .toSingleDefault(Result.success())
        return if (isBetaVersion) {
            parserExtensionsSource.insertAll()
                .andThen(autoRefreshSource)
        } else {
            autoRefreshSource
        }

    }

    private fun parseExtensionsSource(extensionsConfig: ExtensionsConfig) =
        parserExtensionsSource.parseFromRemoteRx(extensionsConfig)
            .flatMapCompletable {
                Completable.complete()
            }.doOnComplete {
                Logger.d(
                    this@AutoRefreshExtensionsChannelWorker,
                    message = "Complete refresh extensions: ${extensionsConfig.sourceUrl}"
                )
                keyValueStorage.saveLastRefreshExtensions(extensionsConfig)
            }

    companion object {
        private const val HOUR = 60 * 60 * 1000L
        const val EXTRA_KEY_VERSION_IS_BETA = "extra:key_version_is_beta"
    }

}