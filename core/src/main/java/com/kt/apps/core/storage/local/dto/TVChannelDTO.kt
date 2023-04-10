package com.kt.apps.core.storage.local.dto

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
class TVChannelDTO(
    var tvGroup: String,
    var logoChannel: String,
    var tvChannelName: String,
    var sourceFrom: String,
    @PrimaryKey
    val channelId: String,
) {
    @Entity(
        primaryKeys = [
            "tvChannelId",
            "url"
        ]
    )
    class TVChannelUrl(
        val src: String? = null,
        val type: String,
        val url: String,
        val tvChannelId: String
    )
}


data class TVChannelWithUrls(
    @Embedded
    val tvChannel: TVChannelDTO,

    @Relation(
        parentColumn = "channelId",
        entityColumn = "tvChannelId"
    )
    val urls: List<TVChannelDTO.TVChannelUrl>
)
