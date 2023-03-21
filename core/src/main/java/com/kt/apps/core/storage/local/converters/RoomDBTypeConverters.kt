package com.kt.apps.core.storage.local.converters

import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.kt.apps.core.storage.local.dto.FootballTeamEntity

class RoomDBTypeConverters {

    @TypeConverter
    fun uriToString(uri: Uri): String = uri.toString()

    @TypeConverter
    fun stringToUri(str: String): Uri = Uri.parse(str)

    @TypeConverter
    fun footballTeamToString(team: FootballTeamEntity): String = Gson().toJson(team)

    @TypeConverter
    fun stringToFootballTeam(str: String): FootballTeamEntity = Gson().fromJson(
        str,
        FootballTeamEntity::class.java
    )

}