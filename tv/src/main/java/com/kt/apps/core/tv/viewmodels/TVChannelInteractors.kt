package com.kt.apps.core.tv.viewmodels

import com.kt.apps.core.tv.usecase.GetChannelLinkStreamById
import com.kt.apps.core.tv.usecase.GetListTVChannel
import com.kt.apps.core.tv.usecase.GetTVChannelLinkStreamFrom
import javax.inject.Inject

data class TVChannelInteractors @Inject constructor(
    val getListChannel: GetListTVChannel,
    val getChannelLinkStream: GetTVChannelLinkStreamFrom,
    val getChannelLinkStreamById: GetChannelLinkStreamById
)