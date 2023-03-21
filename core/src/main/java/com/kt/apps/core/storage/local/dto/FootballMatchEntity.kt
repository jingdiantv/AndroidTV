package com.kt.apps.core.storage.local.dto

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
class FootballMatchEntity(
    val homeTeam: FootballTeamEntity,
    val awayTeam: FootballTeamEntity,
    val kickOffTime: String,
    val kickOffTimeInSecond: Long,
    val statusStream: String,
    val detailPage: String,
    val sourceFrom: String,
    val league: String = "",
    @PrimaryKey
    val matchId: String = detailPage
) {
}

@Entity
data class FootballTeamEntity(
    val name: String,
    @PrimaryKey
    val id: String,
    val league: String,
    val logo: String
)