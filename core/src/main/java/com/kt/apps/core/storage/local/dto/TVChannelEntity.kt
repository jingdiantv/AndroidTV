package com.kt.apps.core.storage.local.dto

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kt.apps.core.utils.mapper.IMapper

@Entity
data class TVChannelEntity(
    val tvGroup: String,
    val logoChannel: Uri,
    val tvChannelName: String,
    val tvChannelWebDetailPage: String,
    val sourceFrom: String,
    @PrimaryKey
    val channelId: String,
    val channelPreviewProviderId: Long = -1
) {
}