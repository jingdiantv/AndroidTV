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

@Dao
abstract class TVChannelUrlDAO {

    @Query("SELECT * FROM TVChannelUrl")
    @Transaction
    abstract fun getListChannelUrl(): Observable<List<TVChannelDTO.TVChannelUrl>>

    @Query("SELECT * FROM TVChannelUrl WHERE tvChannelId=:channelID")
    @Transaction
    abstract fun getListTVChannelByChannelID(channelID: String): Observable<List<TVChannelDTO.TVChannelUrl>>

    @Query("SELECT * FROM TVChannelUrl WHERE src=:dataSource")
    @Transaction
    abstract fun getListTVChannelByDataSource(
        dataSource: String
    ): Observable<List<TVChannelDTO.TVChannelUrl>>

    @Query("SELECT * FROM TVChannelUrl WHERE src=:dataSource AND tvChannelId=:channelID")
    @Transaction
    abstract fun getListTVChannelByDataSource(
        dataSource: String,
        channelID: String
    ): Observable<TVChannelDTO.TVChannelUrl>

    @Query("DELETE FROM TVChannelUrl")
    @Transaction
    abstract fun deleteAll(): Completable

    @Delete
    @Transaction
    abstract fun delete(tvChannelUrl: TVChannelDTO.TVChannelUrl): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    abstract fun insert(list: List<TVChannelDTO.TVChannelUrl>): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    abstract fun insert(list: TVChannelDTO.TVChannelUrl): Completable


}