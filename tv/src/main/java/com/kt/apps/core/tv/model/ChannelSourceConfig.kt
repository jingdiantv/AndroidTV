package com.kt.apps.core.tv.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChannelSourceConfig(
    var baseUrl: String,
    var mainPagePath: String,
    var getLinkStreamPath: String? = null
) : Parcelable {
}