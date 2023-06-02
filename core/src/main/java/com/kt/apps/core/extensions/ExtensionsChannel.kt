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
        return Gson().toJson(this)
    }
}