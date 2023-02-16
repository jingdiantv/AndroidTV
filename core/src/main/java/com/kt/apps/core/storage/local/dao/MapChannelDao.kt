package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kt.apps.core.storage.local.dto.MapChannel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

@Dao
abstract class MapChannelDao {

    @Transaction
    @Query("SELECT * FROM MapChannel")
    abstract fun getChannel(): Observable<List<MapChannel>>

    @Transaction
    @Query("SELECT * FROM MapChannel WHERE channelId = :id")
    abstract fun getChannel(id: String): Observable<MapChannel>

    @Transaction
    @Query("SELECT * FROM MapChannel WHERE fromSource = :fromSource")
    abstract fun getChannelFromSource(fromSource: String): Observable<List<MapChannel>>

    @Transaction
    @Query("SELECT * FROM MapChannel WHERE channelName LIKE :name||'%'")
    abstract fun getChannelByName(name: String): Observable<List<MapChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<MapChannel>): Completable

    @Delete
    abstract fun delete(list: List<MapChannel>): Completable
}