package com.kt.apps.football.model

import android.os.Parcelable
import com.kt.apps.core.utils.removeAllSpecialChars
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
class FootballMatch(
    val homeTeam: FootballTeam,
    val awayTeam: FootballTeam,
    val kickOffTime: String,
    val kickOffTimeInSecond: Long,
    val statusStream: String,
    val detailPage: String,
    val sourceFrom: FootballDataSourceFrom,
    val league: String = "",
    val matchId: String = detailPage
) : Parcelable {
    fun getMatchName(): String {
        return "${homeTeam.name} - ${awayTeam.name}"
    }

    val isLiveMatch: Boolean
        get() {
            val calendar = Calendar.getInstance(Locale.TAIWAN)
            val currentTime = calendar.timeInMillis / 1000
            return (currentTime - kickOffTimeInSecond) > -20 * 60
                    && (currentTime - kickOffTimeInSecond) < 150 * 60
        }

    fun getMatchIdForComment() = "${
        homeTeam.name.trim().removeAllSpecialChars()
    }_${awayTeam.name.trim().removeAllSpecialChars()}"
}