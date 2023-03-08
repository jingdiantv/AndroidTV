package com.kt.apps.football.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FootballMatchWithStreamLink(
    val match: FootballMatch,
    val linkStreams: List<LinkStreamWithReferer>
) : Parcelable {
}