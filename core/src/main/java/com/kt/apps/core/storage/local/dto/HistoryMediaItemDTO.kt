package com.kt.apps.core.storage.local.dto

import androidx.room.Entity
import com.google.android.exoplayer2.MediaItem
import com.kt.apps.core.base.player.AbstractExoPlayerManager

@Entity(
    primaryKeys = ["itemId", "linkPlay"]
)
data class HistoryMediaItemDTO(
    val itemId: String,
    val category: String,
    val displayName: String,
    val thumb: String,
    val currentPosition: Long,
    val contentDuration: Long,
    val isLiveStreaming: Boolean,
    val description: String,
    val linkPlay: String,
    val type: Type,
    val lastViewTime: Long = System.currentTimeMillis()
) {
    enum class Type {
        IPTV, TV, RADIO, FOOTBALL
    }

    override fun toString(): String {
        return "{\n" +
                "itemId: $itemId," +
                "category: $category," +
                "displayName: $displayName," +
                "thumb: $thumb," +
                "currentPosition: $currentPosition," +
                "contentDuration: $contentDuration," +
                "isLiveStreaming: $isLiveStreaming," +
                "description: $description," +
                "linkPlay: $linkPlay," +
                "type: $type," +
                "lastViewTime: $lastViewTime" +
                "}\n"
    }

    companion object {
        fun mapFromMediaItem(
            mediaItem: MediaItem,
            currentPosition: Long,
            contentDuration: Long,
            type: Type = Type.IPTV
        ): HistoryMediaItemDTO {
            val metaData = mediaItem.mediaMetadata
            val requestMetadata = mediaItem.requestMetadata
            return HistoryMediaItemDTO(
                itemId = mediaItem.mediaId,
                category = metaData.albumTitle.toString(),
                displayName = metaData.displayTitle.toString(),
                thumb = "${metaData.artworkUri}",
                currentPosition = currentPosition,
                contentDuration = contentDuration,
                isLiveStreaming = false,
                description = metaData.description.toString(),
                linkPlay = "${requestMetadata.mediaUri}",
                type = type
            )
        }
    }
}