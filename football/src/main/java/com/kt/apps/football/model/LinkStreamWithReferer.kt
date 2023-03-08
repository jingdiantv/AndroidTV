package com.kt.apps.football.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LinkStreamWithReferer(
    val m3u8Link: String,
    val referer: String
): Parcelable {
    var token: String? = null
    var host: String? = null
}