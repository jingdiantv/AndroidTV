package com.kt.apps.media.xemtv.ui.extensions

import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.extensions.ExtensionsChannel
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
    private val _extensionsChannelListCache by lazy {
        mutableMapOf<String, List<ExtensionsChannel>>()
    }

    val channelListCache: Map<String, List<ExtensionsChannel>>
        get() = _extensionsChannelListCache


    fun appendExtensionsCache(id: String, channelList: List<ExtensionsChannel>) {
        Logger.e(this@ExtensionsViewModel, message = "id = $id")
        _extensionsChannelListCache[id] = channelList
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
}