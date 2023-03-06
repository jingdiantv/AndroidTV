package com.kt.apps.football.model

data class FootballMatchWithStreamLink(
    val match: FootballMatch,
    val linkStreams: List<LinkStreamWithReferer>
) {
}