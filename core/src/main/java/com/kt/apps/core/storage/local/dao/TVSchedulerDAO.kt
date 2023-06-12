package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kt.apps.core.extensions.model.TVScheduler
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
abstract class TVSchedulerDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<TVScheduler>): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: TVScheduler): Completable

    @Update
    abstract fun update(list: List<TVScheduler>): Completable

    @Delete
    abstract fun delete(channel: TVScheduler): Completable

    @Delete
    abstract fun delete(listChannel: List<TVScheduler>): Completable

    @Transaction
    @Query("SELECT * FROM tvscheduler WHERE extensionsConfigId=:configId")
    abstract fun getAllByExtensionlId(configId: String): Single<List<TVScheduler>>

    @Query(
        "DELETE FROM tvscheduler " +
                "WHERE extensionsConfigId=:configId " +
                "AND epgUrl=:epgUrl"
    )
    abstract fun deleteProgramByConfig(
        configId: String,
        epgUrl: String
    ): Completable
}