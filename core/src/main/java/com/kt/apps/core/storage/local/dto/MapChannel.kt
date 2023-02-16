package com.kt.apps.core.storage.local.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class MapChannel(
    @PrimaryKey
    var channelId: String,
    var channelName: String,
    var fromSource: String,
    var channelGroup: String
) {
}