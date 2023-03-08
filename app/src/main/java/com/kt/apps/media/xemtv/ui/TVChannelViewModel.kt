package com.kt.apps.media.xemtv.ui

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.usecase.GetChannelLinkStreamById
import com.kt.apps.core.tv.usecase.GetListTVChannel
import com.kt.apps.core.tv.usecase.GetTVChannelLinkStreamFrom
import io.reactivex.rxjava3.disposables.Disposable
import javax.inject.Inject

data class TVChannelInteractors @Inject constructor(
    val getListChannel: GetListTVChannel,
    val getChannelLinkStream: GetTVChannelLinkStreamFrom,
    val getChannelLinkStreamById: GetChannelLinkStreamById
)

class TVChannelViewModel @Inject constructor(
    private val interactors: TVChannelInteractors
) : BaseViewModel() {

    private val _listTvChannelLiveData by lazy {
        MutableLiveData<DataState<List<TVChannel>>>()
    }

    val tvChannelLiveData: LiveData<DataState<List<TVChannel>>>
        get() = _listTvChannelLiveData

    fun getListTVChannel(forceRefresh: Boolean) {
        if (!forceRefresh && interactors.getListChannel.cacheData != null) {
            _listTvChannelLiveData.postValue(DataState.Success(interactors.getListChannel.cacheData!!))
            return
        }
        val finalList = mutableListOf<TVChannel>()
        _listTvChannelLiveData.postValue(DataState.Loading())

        add(
            interactors.getListChannel(forceRefresh)
                .subscribe({
                    Logger.d(this, message = "Response data: ${Gson().toJson(it)}")
                    finalList.addAll(it)
                }, {
                    Logger.e(this, exception = it)
                    _listTvChannelLiveData.postValue(DataState.Error(it))
                }, {
                    _listTvChannelLiveData.postValue(DataState.Success(finalList))
                })
        )
    }

    private var lastTVStreamLinkTask: Disposable? = null
    private val _tvWithLinkStreamLiveData by lazy { MutableLiveData<DataState<TVChannelLinkStream>>() }
    val tvWithLinkStreamLiveData: LiveData<DataState<TVChannelLinkStream>>
        get() = _tvWithLinkStreamLiveData

    fun getLinkStreamForChannel(tvDetail: TVChannel, isBackup: Boolean = false) {
        _tvWithLinkStreamLiveData.postValue(DataState.Loading())
        if (lastTVStreamLinkTask?.isDisposed != true) {
            lastTVStreamLinkTask?.dispose()
        }

        lastTVStreamLinkTask = interactors.getChannelLinkStream(tvDetail, isBackup)
            .subscribe({
                Logger.d(this, message = Gson().toJson(it))
                _tvWithLinkStreamLiveData.postValue(DataState.Success(it))
            }, {
                Logger.e(this, exception = it)
                _tvWithLinkStreamLiveData.postValue(DataState.Error(it))
            })

        add(lastTVStreamLinkTask!!)
    }

    fun playTvByDeepLinks(uri: Uri) {
        !(uri.host?.contentEquals(Constants.DEEPLINK_HOST) ?: return)
        val lastPath = uri.pathSegments.last() ?: return
        Logger.d(
            this, message = "play by deeplink: {" +
                    "uri: $uri" +
                    "}"
        )

        if (lastTVStreamLinkTask?.isDisposed != true) {
            lastTVStreamLinkTask?.dispose()
        }

        lastTVStreamLinkTask = interactors.getChannelLinkStreamById(lastPath)
            .subscribe({
                _tvWithLinkStreamLiveData.postValue(DataState.Success(it))
                Logger.d(
                    this, message = "play by deeplink result: {" +
                            "uri: $uri, " +
                            "channel: $it" +
                            "}"
                )
            }, {
                _tvWithLinkStreamLiveData.postValue(DataState.Error(it))
                Logger.e(this, exception = it)
            })
        add(lastTVStreamLinkTask!!)
    }


    init {
        instance++
        Logger.d(this, message = "TVChannelViewModel instance count: $instance")
    }

    companion object {
        private var instance = 0
    }
}