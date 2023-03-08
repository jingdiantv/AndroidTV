package com.kt.apps.football.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FootballTeam(
    val name: String,
    val id: String,
    val league: String,
    val logo: String
): Parcelable {
}