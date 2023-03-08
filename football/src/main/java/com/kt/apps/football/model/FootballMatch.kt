package com.kt.apps.football.model

import android.os.Parcelable
import com.kt.apps.core.utils.removeAllSpecialChars
import kotlinx.parcelize.Parcelize

@Parcelize
class FootballMatch(
    val homeTeam: FootballTeam,
    val awayTeam: FootballTeam,
    val kickOffTime: String,
    val statusStream: String,
    val detailPage: String,
    val sourceFrom: FootballDataSourceFrom,
    val league: String = "",
    val matchId: String = detailPage
) : Parcelable {
    fun getMatchName(): String {
        return "${homeTeam.name} - ${awayTeam.name}"
    }

    fun getMatchIdForComment() = "${
        homeTeam.name.trim().removeAllSpecialChars()
    }_${awayTeam.name.trim().removeAllSpecialChars()}"
}