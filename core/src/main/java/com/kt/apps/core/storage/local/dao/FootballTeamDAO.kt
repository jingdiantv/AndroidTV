package com.kt.apps.core.storage.local.dao

import androidx.room.*
import com.kt.apps.core.storage.local.dto.FootballTeamEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

@Dao
abstract class FootballTeamDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(footballTeam: FootballTeamEntity): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<FootballTeamEntity>): Completable

    @Update
    abstract fun update(footballTeam: FootballTeamEntity): Completable

    @Delete
    abstract fun delete(footballTeam: FootballTeamEntity): Completable

    @Query("SELECT * from FootballTeamEntity")
    abstract fun getAll(): Observable<List<FootballTeamEntity>>

}