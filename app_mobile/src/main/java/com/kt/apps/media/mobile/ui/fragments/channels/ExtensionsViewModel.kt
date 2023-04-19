package com.kt.apps.media.mobile.ui.fragments.channels

import android.provider.ContactsContract.Data
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.media.mobile.di.AppScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
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
    init {
        compositeDisposable.add(
            observableData
                .subscribe {
                    extensionsConfigs.postValue(it)
                }
        )
        compositeDisposable.add(
            observableData
                .doOnSubscribe {
                    extensionsChannelData.postValue(DataState.Loading())
                }
                .flatMapIterable { x -> x }
                .flatMap { parserExtensionsSource.parseFromRemoteRx(it) }
                .subscribe ({
                    extensionsChannelData.postValue(DataState.Success(it))
                }, {
                    extensionsChannelData.postValue(DataState.Error(it))
                })
        )
    }

}