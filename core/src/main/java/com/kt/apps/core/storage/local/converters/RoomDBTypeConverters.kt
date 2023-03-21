package com.kt.apps.core.storage.local.converters

import android.net.Uri
import androidx.room.TypeConverter

class RoomDBTypeConverters {

    @TypeConverter
    fun uriToString(uri: Uri): String = uri.toString()

    @TypeConverter
    fun stringToUri(str: String): Uri = Uri.parse(str)

}