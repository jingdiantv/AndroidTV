package com.kt.apps.media.mobile.ui.fragments.models

import android.net.Uri
import androidx.work.WorkManager
import com.kt.apps.core.Constants
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.viewmodels.BaseTVChannelViewModel
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.core.utils.isShortLink
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TVChannelViewModel @Inject constructor(
    interactors: TVChannelInteractors,
    private val workManager: WorkManager
) : BaseTVChannelViewModel(interactors) {

    override fun onFetchTVListSuccess(listChannel: List<TVChannel>) {
        super.onFetchTVListSuccess(listChannel)
    }

    fun playMobileTvByDeepLinks(uri: Uri) : Boolean {
        !(uri.host?.contentEquals(Constants.DEEPLINK_HOST) ?: return false)
        val lastPath = uri.pathSegments.last() ?: return false
        _tvWithLinkStreamLiveData.postValue(DataState.Loading())
        super.playTvByDeepLinks(uri)
        return true
    }

    fun  getExtensionChannel(tvChannel: ExtensionsChannel) {
        val linkToPlay = tvChannel.tvStreamLink
        if (linkToPlay.isShortLink()) {
            compositeDisposable.add(
                Observable.just(linkToPlay.expandUrl())
                    .observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        _tvWithLinkStreamLiveData.postValue(
                            DataState.Success(TVChannelLinkStream(
                                TVChannel.fromChannelExtensions(tvChannel),
                                arrayListOf(it)
                            ))
                        )
                    }
            )
        } else {
            _tvWithLinkStreamLiveData.postValue(DataState.Loading())
            compositeDisposable.add(
                Observable.just(linkToPlay)
                    .delay(500, TimeUnit.MILLISECONDS)
                    .observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        _tvWithLinkStreamLiveData.postValue(
                            DataState.Success(TVChannelLinkStream(
                                TVChannel.fromChannelExtensions(tvChannel),
                                arrayListOf(linkToPlay)
                            ))
                        )
                    }
            )
        }
    }

    companion object {
        private var instance = 0
    }
}