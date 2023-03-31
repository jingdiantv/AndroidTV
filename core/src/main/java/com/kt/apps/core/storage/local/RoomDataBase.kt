package com.kt.apps.core.storage.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.storage.local.converters.RoomDBTypeConverters
import com.kt.apps.core.storage.local.dao.*
import com.kt.apps.core.storage.local.dto.FootballMatchEntity
import com.kt.apps.core.storage.local.dto.FootballTeamEntity
import com.kt.apps.core.storage.local.dto.MapChannel
import com.kt.apps.core.storage.local.dto.TVChannelEntity

@TypeConverters(
    RoomDBTypeConverters::class
)
@Database(
    entities = [
        MapChannel::class,
        TVChannelEntity::class,
        FootballMatchEntity::class,
        FootballTeamEntity::class,
        ExtensionsConfig::class
    ],
    version = 3
)
abstract class RoomDataBase : RoomDatabase() {
    abstract fun mapChannelDao(): MapChannelDao
    abstract fun tvChannelEntityDao(): TVChannelDAO
    abstract fun footballTeamDao(): FootballTeamDAO
    abstract fun footballMatchDao(): FootballMatchDAO
    abstract fun extensionsConfig(): ExtensionsConfigDAO

    companion object {
        @Volatile
        var INSTANCE: RoomDataBase? = null
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context, RoomDataBase::class.java, "TV")
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE IF NOT EXISTS `TVChannelEntity` (`tvGroup` TEXT NOT NULL, `logoChannel` TEXT NOT NULL, `tvChannelName` TEXT NOT NULL, `tvChannelWebDetailPage` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `channelId` TEXT NOT NULL, `channelPreviewProviderId` INTEGER NOT NULL, PRIMARY KEY(`channelId`))")
                        database.execSQL("CREATE TABLE IF NOT EXISTS `FootballMatchEntity` (`homeTeam` TEXT NOT NULL, `awayTeam` TEXT NOT NULL, `kickOffTime` TEXT NOT NULL, `kickOffTimeInSecond` INTEGER NOT NULL, `statusStream` TEXT NOT NULL, `detailPage` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `league` TEXT NOT NULL, `matchId` TEXT NOT NULL, PRIMARY KEY(`matchId`))")
                        database.execSQL("CREATE TABLE IF NOT EXISTS `FootballTeamEntity` (`name` TEXT NOT NULL, `id` TEXT NOT NULL, `league` TEXT NOT NULL, `logo` TEXT NOT NULL, PRIMARY KEY(`id`))")
                        database.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                    }
                })
                .addMigrations(object : Migration(2, 3) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("CREATE TABLE IF NOT EXISTS `ExtensionsConfig` (`sourceName` TEXT NOT NULL, `sourceUrl` TEXT NOT NULL, `type` TEXT NOT NULL, PRIMARY KEY(`sourceUrl`))")
                    }
                })
                .build()
                .also {
                    INSTANCE = it
                }
        }
    }
}