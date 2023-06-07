package com.kt.apps.media.xemtv.ui.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.media.xemtv.di.AppScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

@AppScope
class ExtensionsViewModel @Inject constructor(
    private val parserExtensionsSource: ParserExtensionsSource,
    private val roomDataBase: RoomDataBase
) : BaseViewModel() {

    private val _totalExtensionsConfig by lazy {
        MutableLiveData<DataState<List<ExtensionsConfig>>>()
    }

    val totalExtensionsConfig: LiveData<DataState<List<ExtensionsConfig>>>
        get() = _totalExtensionsConfig


    private val _extensionsChannelListCache by lazy {
        mutableMapOf<String, List<ExtensionsChannel>>()
    }

    val channelListCache: Map<String, List<ExtensionsChannel>>
        get() = _extensionsChannelListCache


    fun appendExtensionsCache(id: String, channelList: List<ExtensionsChannel>) {
        Logger.e(this@ExtensionsViewModel, message = "id = $id")
        _extensionsChannelListCache[id] = channelList
    }

    fun loadAllListExtensionsChannelConfig(refreshCache: Boolean = false) {
        if (!refreshCache && _totalExtensionsConfig.value is DataState.Success) {
            _totalExtensionsConfig.postValue(_totalExtensionsConfig.value)
            return
        }

        _totalExtensionsConfig.postValue(DataState.Loading())
        add(
            roomDataBase.extensionsConfig()
                .getAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    _totalExtensionsConfig.postValue(DataState.Success(it))
                    Logger.d(this@ExtensionsViewModel, message = "addExtensionsPage")
                }, {
                    _totalExtensionsConfig.postValue(DataState.Error(it))
                })
        )
    }

    fun parseExtensionByID(extensionsID: String) {
        add(
            roomDataBase.extensionsConfig()
                .getExtensionById(extensionsID)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    parserExtensionsSource.parseFromRemoteRx(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ tvList ->
                    appendExtensionsCache(extensionsID, tvList)
                }, {
                    Logger.e(this, exception = it)
                })
        )
    }
}