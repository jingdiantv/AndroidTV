package com.kt.apps.media.xemtv.ui.extensions

import com.kt.apps.core.base.viewmodels.BaseExtensionsViewModel
import com.kt.apps.core.base.viewmodels.HistoryIteractors
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.repository.IMediaHistoryRepository
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.usecase.GetCurrentProgrammeForChannel
import com.kt.apps.core.usecase.GetListProgrammeForChannel
import com.kt.apps.media.xemtv.di.AppScope
import javax.inject.Inject

@AppScope
class ExtensionsViewModel @Inject constructor(
    private val parserExtensionsSource: ParserExtensionsSource,
    private val roomDataBase: RoomDataBase,
    private val getCurrentProgrammeForChannel: GetCurrentProgrammeForChannel,
    private val getListProgrammeForChannel: GetListProgrammeForChannel,
    private val actionLogger: IActionLogger,
    private val storage: IKeyValueStorage,
    private val historyIteractors: HistoryIteractors
) : BaseExtensionsViewModel(
    parserExtensionsSource,
    roomDataBase,
    getCurrentProgrammeForChannel,
    getListProgrammeForChannel,
    actionLogger,
    storage,
    historyIteractors
) {

}