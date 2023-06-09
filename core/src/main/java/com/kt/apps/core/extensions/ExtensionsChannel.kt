package com.kt.apps.core.extensions

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity(
    primaryKeys = [
        "channelId",
        "tvStreamLink"
    ]
)
data class ExtensionsChannel(
    var tvGroup: String,
    val logoChannel: String,
    val tvChannelName: String,
    val tvStreamLink: String,
    val sourceFrom: String,
    val channelId: String,
    val channelPreviewProviderId: Long = -1,
    val isHls: Boolean,
    val catchupSource: String = "",
    val userAgent: String = "",
    val referer: String = "",
    val props: Map<String, String> = mapOf(),
    val extensionSourceId: String
) : Parcelable {
    override fun toString(): String {
        return "{" +
                "tvGroup=$tvGroup,\n" +
                "logoChannel=$logoChannel,\n" +
                "tvChannelName=$tvChannelName,\n" +
                "tvStreamLink=$tvStreamLink,\n" +
                "sourceFrom=$sourceFrom,\n" +
                "channelPreviewProviderId=$channelPreviewProviderId,\n" +
                "isHls=$isHls,\n" +
                "catchupSource=$catchupSource,\n" +
                "userAgent=$userAgent,\n" +
                "referer=$referer,\n" +
                "extensionSourceId=$extensionSourceId,\n" +
                "props=$props," +
                "}"
    }
}