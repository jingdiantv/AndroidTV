package com.kt.apps.core.tv.model

import android.os.Parcelable
import androidx.room.PrimaryKey
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.storage.local.dto.TVChannelEntity
import kotlinx.parcelize.Parcelize

@Parcelize
class TVChannel(
    var tvGroup: String,
    var logoChannel: String,
    var tvChannelName: String,
    var tvChannelWebDetailPage: String,
    var sourceFrom: String,
    @PrimaryKey
    val channelId: String,
    val urls: List<Url> = listOf(),
    var isFreeContent: Boolean = true,
    var referer: String = ""
) : Parcelable {

    @Parcelize
    data class Url(
        val dataSource: String? = null,
        val type: String,
        var url: String
    ) : Parcelable {
        val isHls: Boolean
            get() = url.contains("m3u8")
    }

    val isRadio: Boolean
        get() = radioGroup.contains(tvGroup)

    val tvGroupLocalName: String
        get() = TVChannelGroup.valueOf(tvGroup).value

    val isHls: Boolean
        get() = tvChannelWebDetailPage.contains("m3u8")
                || tvGroup != TVChannelGroup.VOV.name


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
            logoChannel = entity.logoChannel.toString(),
            urls = listOf()
        )

        fun fromChannelExtensions(entity: ExtensionsChannel) = TVChannel(
            tvChannelName = entity.tvChannelName,
            tvGroup = entity.tvGroup,
            tvChannelWebDetailPage = entity.tvStreamLink,
            sourceFrom = entity.sourceFrom,
            channelId = entity.channelId,
            logoChannel = entity.logoChannel,
            urls = listOf()

        )
    }
}