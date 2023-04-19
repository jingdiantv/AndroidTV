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
import javax.inject.Inject

@AppScope
class ExtensionsViewModel @Inject constructor(
    private val parserExtensionsSource: ParserExtensionsSource,
    private val roomDataBase: RoomDataBase
) : BaseViewModel() {
    val extensionsConfigs:  MutableLiveData<List<ExtensionsConfig>> by lazy {
        MutableLiveData()
    }
    val extensionsChannelData: MutableLiveData<DataState<List<ExtensionsChannel>>> by lazy {
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
            observableData.flatMap {
                if (it.isEmpty()) {
                    Observable.just(emptyList())
                } else {
                    Observable.just(it)
                        .flatMapIterable { x -> x }
                        .flatMap { x -> parserExtensionsSource.parseFromRemoteRx(x) }
                }
            }
                .subscribe ({
                    extensionsChannelData.postValue(DataState.Success(it))
                }, {
                    extensionsChannelData.postValue(DataState.Error(it))
                })

        )
    }

}