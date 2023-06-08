package com.kt.apps.media.mobile.ui.fragments.models

import android.net.Uri
import androidx.work.WorkManager
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.viewmodels.BaseTVChannelViewModel
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.core.utils.isShortLink
import com.kt.apps.media.mobile.di.AppScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AppScope
class ChannelViewModel @Inject constructor(
    interactors: TVChannelInteractors,
    private val workManager: WorkManager,
    private val parserExtensionsSource: ParserExtensionsSource,
    private val roomDataBase: RoomDataBase
): BaseViewModel() {
    val tvChannelViewModel by lazy {
        TVChannelViewModel(interactors, workManager)
    }

    val extensionsViewModel by lazy {
        ExtensionsViewModel(parserExtensionsSource, roomDataBase)
    }
}