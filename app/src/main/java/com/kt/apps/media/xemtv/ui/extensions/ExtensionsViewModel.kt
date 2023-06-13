package com.kt.apps.media.xemtv.ui.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.usecase.GetCurrentProgrammeForChannel
import com.kt.apps.core.usecase.GetListProgrammeForChannel
import com.kt.apps.media.xemtv.di.AppScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.ref.WeakReference
import javax.inject.Inject

@AppScope
class ExtensionsViewModel @Inject constructor(
    private val parserExtensionsSource: ParserExtensionsSource,
    private val roomDataBase: RoomDataBase,
    private val getCurrentProgrammeForChannel: GetCurrentProgrammeForChannel,
    private val getListProgrammeForChannel: GetListProgrammeForChannel
) : BaseViewModel() {

    private val _totalExtensionsConfig by lazy {
        MutableLiveData<DataState<List<ExtensionsConfig>>>()
    }

    val totalExtensionsConfig: LiveData<DataState<List<ExtensionsConfig>>>
        get() = _totalExtensionsConfig


    private val _extensionsChannelListCache by lazy {
        mutableMapOf<String, WeakReference<List<ExtensionsChannel>>>()
    }

    init {
        loadAllListExtensionsChannelConfig(true)
    }

    val channelListCache: Map<String, WeakReference<List<ExtensionsChannel>>>
        get() = _extensionsChannelListCache


    fun appendExtensionsCache(id: String, channelList: List<ExtensionsChannel>) {
        Logger.e(this@ExtensionsViewModel, message = "id = $id")
        _extensionsChannelListCache[id] = WeakReference(channelList)
    }

    fun loadChannelForConfig(configId: String): LiveData<DataState<List<ExtensionsChannel>>> {
        if (_extensionsChannelListCache[configId] != null
            && !_extensionsChannelListCache[configId]!!.get().isNullOrEmpty()
        ) {
            return MutableLiveData(DataState.Success(_extensionsChannelListCache[configId]!!.get()!!))
        }
        val liveData = MutableLiveData<DataState<List<ExtensionsChannel>>>()
        liveData.postValue(DataState.Loading())
        add(
            roomDataBase.extensionsConfig()
                .getExtensionById(configId)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    parserExtensionsSource.parseFromRemoteRx(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .retry { t1, t2 ->
                    return@retry t1 < 3
                }
                .subscribe({ tvList ->
                    appendExtensionsCache(configId, tvList)
                    liveData.postValue(DataState.Success(tvList))
                }, {
                    liveData.postValue(DataState.Error(it))
                    Logger.e(this, exception = it)
                })
        )
        return liveData
    }

    private val _programmeForChannelLiveData by lazy {
        MutableLiveData<DataState<TVScheduler.Programme>>()
    }

    val programmeForChannelLiveData: LiveData<DataState<TVScheduler.Programme>>
        get() = _programmeForChannelLiveData

    fun loadProgramForChannel(channel: ExtensionsChannel) {
        add(
            getCurrentProgrammeForChannel.invoke(channel)
                .subscribe({
                    _programmeForChannelLiveData.postValue(DataState.Success(it))
                }, {
                    _programmeForChannelLiveData.postValue(DataState.Error(it))
                })
        )
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
                .flatMapMaybe {
                    parserExtensionsSource.parseFromRemoteMaybe(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ tvList ->
                    appendExtensionsCache(extensionsID, tvList)
                }, {
                    Logger.e(this, exception = it)
                })
        )
    }

    fun insertDefaultSource() {
        add(
            parserExtensionsSource.insertAll()
                .subscribe({
                    Logger.d(this@ExtensionsViewModel, message = "insertDefaultSourceSuccess")
                }, {
                    Logger.e(this@ExtensionsViewModel, exception = it)
                })
        )
    }
}