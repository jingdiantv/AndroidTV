package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.storage.local.dto.TVChannelWithUrls
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

@Dao
abstract class TVChannelUrlDAO {

    @Query("SELECT * FROM TVChannelUrl")
    abstract fun getListChannelUrl(): Observable<List<TVChannelDTO.TVChannelUrl>>

    @Query("SELECT * FROM TVChannelUrl WHERE tvChannelId=:channelID")
    abstract fun getListTVChannelByChannelID(channelID: String): Observable<List<TVChannelDTO.TVChannelUrl>>

    @Query("SELECT * FROM TVChannelUrl WHERE src=:dataSource")
    abstract fun getListTVChannelByDataSource(
        dataSource: String
    ): Observable<List<TVChannelDTO.TVChannelUrl>>

    @Query("SELECT * FROM TVChannelUrl WHERE src=:dataSource AND tvChannelId=:channelID")
    abstract fun getListTVChannelByDataSource(
        dataSource: String,
        channelID: String
    ): Observable<TVChannelDTO.TVChannelUrl>

    @Query("DELETE FROM TVChannelUrl")
    abstract fun deleteAll(): Completable

    @Delete
    abstract fun delete(tvChannelUrl: TVChannelDTO.TVChannelUrl): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<TVChannelDTO.TVChannelUrl>): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: TVChannelDTO.TVChannelUrl): Completable


}