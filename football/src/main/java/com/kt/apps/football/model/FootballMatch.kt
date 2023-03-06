package com.kt.apps.football.model

import com.kt.apps.core.utils.removeAllSpecialChars

class FootballMatch(
    val homeTeam: FootballTeam,
    val awayTeam: FootballTeam,
    val kickOffTime: String,
    val statusStream: String,
    val detailPage: String,
    val sourceFrom: FootballDataSourceFrom,
    val league: String = "",
    val matchId: String = detailPage
) {
    fun getMatchIdForComment() = "${
        homeTeam.name.trim().removeAllSpecialChars()
    }_${awayTeam.name.trim().removeAllSpecialChars()}"
}