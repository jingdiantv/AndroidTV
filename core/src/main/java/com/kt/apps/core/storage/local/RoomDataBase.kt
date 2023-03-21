package com.kt.apps.core.storage.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kt.apps.core.storage.local.converters.RoomDBTypeConverters
import com.kt.apps.core.storage.local.dao.MapChannelDao
import com.kt.apps.core.storage.local.dao.TVChannelDAO
import com.kt.apps.core.storage.local.dto.MapChannel
import com.kt.apps.core.storage.local.dto.TVChannelEntity

@TypeConverters(
    RoomDBTypeConverters::class
)
@Database(
    entities = [
        MapChannel::class,
        TVChannelEntity::class
    ],
    version = 1
)
abstract class RoomDataBase : RoomDatabase() {
    abstract fun mapChannelDao(): MapChannelDao

    abstract fun tvChannelEntityDao(): TVChannelDAO

    companion object {
        @Volatile
        var INSTANCE: RoomDataBase? = null
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context, RoomDataBase::class.java, "TV")
                .build()
                .also {
                    INSTANCE = it
                }
        }
    }
}