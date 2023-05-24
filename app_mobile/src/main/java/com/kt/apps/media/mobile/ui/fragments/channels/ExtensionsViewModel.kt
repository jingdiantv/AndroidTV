package com.kt.apps.media.mobile.ui.fragments.channels

import android.provider.ContactsContract.Data
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.di.AppScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import java.security.cert.Extension
import javax.inject.Inject

typealias ExtensionResult = Map<ExtensionsConfig, List<ExtensionsChannel>>
@AppScope
class ExtensionsViewModel @Inject constructor(
    private val parserExtensionsSource: ParserExtensionsSource,
    private val roomDataBase: RoomDataBase
) : BaseViewModel() {
    val extensionsConfigs: MutableLiveData<List<ExtensionsConfig>> by lazy {
        MutableLiveData()
    }
    val extensionsChannelData: MutableLiveData<DataState<ExtensionResult>> by lazy {
        MutableLiveData()
    }
    private val observableData: Observable<List<ExtensionsConfig>>
        get() = roomDataBase.extensionsConfig()
            .getAll()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                extensionsChannelData.postValue(DataState.Loading())
            }

    init {
        compositeDisposable.add(
            observableData
                .subscribe {
                    extensionsConfigs.postValue(it)
                }
        )
        compositeDisposable.add(
            observableData.flatMap { it ->
                if (it.isEmpty()) {
                    Observable.just(emptyMap())
                } else {
                    val listObs: Iterable<Observable<Pair<ExtensionsConfig, List<ExtensionsChannel>>>> =
                        it.map { exConfig ->
                            parserExtensionsSource.parseFromRemoteRx(exConfig)
                                .map { list ->
                                    Pair(exConfig, list)
                                }
                        }
                    Observable.combineLatest(listObs) { listResult ->
                        val result: MutableMap<ExtensionsConfig, List<ExtensionsChannel>> = mutableMapOf()
                        listResult.forEach {
                            (it as? Pair<*, *>)?.run {
                                val key = this.first as? ExtensionsConfig ?: return@run
                                val item = this.second as? List<*> ?: return@run
                                result[key] = item.filterIsInstance<ExtensionsChannel>()
                            }
                        }
                        result
                    }
                }
            }
                .subscribe({
                    extensionsChannelData.postValue(DataState.Success(it))
                }, {
                    extensionsChannelData.postValue(DataState.Error(it))
                })

        )
    }

    fun deleteExtension(sourceName: String) {
        extensionsConfigs.value?.find {
            it.sourceName == sourceName
        }?.run {
            compositeDisposable.add(
                roomDataBase.extensionsConfig().delete(this)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe {
                        Log.d(TAG, "deleteExtension: Delete succeed")
                    }
            )
        }
    }

}

inline fun <reified T> merge(vararg arrays: Array<T>): Array<T> {
    val list: MutableList<T> = ArrayList()
    for (array in arrays) {
        list.addAll(array.map { i -> i })
    }
    return list.toTypedArray()
}