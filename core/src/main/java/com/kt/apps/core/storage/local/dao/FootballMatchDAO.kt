package com.kt.apps.core.storage.local.dao

import androidx.room.*
import com.kt.apps.core.storage.local.dto.FootballMatchEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

@Dao
abstract class FootballMatchDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(match: FootballMatchEntity): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<FootballMatchEntity>): Completable

    @Update
    abstract fun update(match: FootballMatchEntity): Completable

    @Delete
    abstract fun delete(match: FootballMatchEntity): Completable

    @Query("SELECT * from FootballMatchEntity")
    abstract fun getAll(): Observable<List<FootballMatchEntity>>
}