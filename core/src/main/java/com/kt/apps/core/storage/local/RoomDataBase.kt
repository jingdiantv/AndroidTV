package com.kt.apps.core.storage.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kt.apps.core.storage.local.dao.MapChannelDao
import com.kt.apps.core.storage.local.dto.MapChannel

@Database(
    entities = [MapChannel::class],
    version = 1
)
abstract class RoomDataBase : RoomDatabase() {
    abstract fun mapChannelDao(): MapChannelDao

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