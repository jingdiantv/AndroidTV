package com.kt.apps.core.tv.model

import android.os.Parcelable
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.storage.local.dto.TVChannelEntity
import kotlinx.parcelize.Parcelize

@Parcelize
class TVChannel(
    var tvGroup: String,
    var logoChannel: String,
    var tvChannelName: String,
    var tvChannelWebDetailPage: String,
    var sourceFrom: String,
    val channelId: String
) : Parcelable {

    val isRadio: Boolean
        get() = radioGroup.contains(tvGroup)

    val tvGroupLocalName: String
        get() = TVChannelGroup.valueOf(tvGroup).value

    val isHls: Boolean
        get() = tvChannelWebDetailPage.contains("m3u8")
                || tvGroup != TVChannelGroup.VOV.name

    var isFreeContent: Boolean = true

    override fun toString(): String {
        return "{" +
                "tvGroup: $tvGroup," +
                "logoChannel: $logoChannel," +
                "tvChannelName: $tvChannelName," +
                "tvChannelWebDetailPage: $tvChannelWebDetailPage," +
                "sourceFrom: $sourceFrom," +
                "channelId: $channelId" +
                "}"
    }

    override fun equals(other: Any?): Boolean {
        if (other is TVChannel) {
            return other.channelId.equals(channelId, ignoreCase = true)
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = tvGroup.hashCode()
        result = 31 * result + logoChannel.hashCode()
        result = 31 * result + tvChannelName.hashCode()
        result = 31 * result + tvChannelWebDetailPage.hashCode()
        result = 31 * result + sourceFrom.hashCode()
        result = 31 * result + channelId.hashCode()
        return result
    }

    companion object {
        private val radioGroup by lazy {
            listOf(TVChannelGroup.VOV.name, TVChannelGroup.VOH.name)
        }

        fun fromEntity(entity: TVChannelEntity) = TVChannel(
            tvChannelName = entity.tvChannelName,
            tvGroup = entity.tvGroup,
            tvChannelWebDetailPage = entity.tvChannelWebDetailPage,
            sourceFrom = entity.sourceFrom,
            channelId = entity.channelId,
            logoChannel = entity.logoChannel.toString()
        )

        fun fromChannelExtensions(entity: ParserExtensionsSource.ExtensionsChannel) = TVChannel(
            tvChannelName = entity.tvChannelName,
            tvGroup = entity.tvGroup,
            tvChannelWebDetailPage = entity.tvStreamLink,
            sourceFrom = entity.sourceFrom,
            channelId = entity.channelId,
            logoChannel = entity.logoChannel.toString(),

        )
    }
}