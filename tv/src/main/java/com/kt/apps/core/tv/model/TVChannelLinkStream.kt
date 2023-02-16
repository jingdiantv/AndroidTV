package com.kt.apps.core.tv.model

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize

@Parcelize
data class TVChannelLinkStream(
    val channel: TVChannel,
    val linkStream: List<String>
) : Parcelable {
    override fun toString(): String {
        return "{" +
                "channel: $channel," +
                "linkStream: ${Gson().toJson(linkStream)}" +
                "}"
    }

    @Parcelize
    data class StreamResolution(
        val type: String,
        val linkStream: String
    ) : Parcelable
}