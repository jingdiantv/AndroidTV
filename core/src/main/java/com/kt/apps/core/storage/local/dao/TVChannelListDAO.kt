package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.storage.local.dto.TVChannelWithUrls
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
abstract class TVChannelListDAO {

    @Query("SELECT * FROM TVChannelDTO")
    @Transaction
    abstract fun getListChannelWithUrl(): Single<List<TVChannelWithUrls>>

    @Query("SELECT * FROM TVChannelDTO WHERE channelId=:channelID")
    @Transaction
    abstract fun getChannelWithUrl(channelID: String): Observable<TVChannelWithUrls>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    abstract fun insertListChannel(
        listChannel: List<TVChannelDTO>
    ): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    abstract fun insertListChannel(
        channel: TVChannelDTO
    ): Completable


    @Delete
    @Transaction
    abstract fun delete(listChannel: List<TVChannelDTO>): Completable

}