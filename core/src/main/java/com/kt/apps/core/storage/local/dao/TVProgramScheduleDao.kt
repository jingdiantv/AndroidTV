package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.model.TVScheduler
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.util.stream.Stream

@Dao
abstract class TVProgramScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<TVScheduler.Programme>): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertSync(list: List<TVScheduler.Programme>)

    @Update
    abstract fun update(list: List<TVScheduler.Programme>): Completable

    @Delete
    abstract fun delete(channel: TVScheduler.Programme): Completable

    @Delete
    abstract fun delete(listChannel: List<TVScheduler.Programme>): Completable

    @Transaction
    @Query("SELECT * FROM programme WHERE channel=:channelId")
    abstract fun getAllProgramByChannelId(channelId: String): Single<List<TVScheduler.Programme>>

    @Transaction
    @Query("SELECT * FROM programme WHERE channel LIKE '%' ||:channelId || '%'")
    abstract fun getAllLikeChannelId(channelId: String): Single<List<TVScheduler.Programme>>

    @Query(
        "DELETE FROM programme " +
                "WHERE extensionsConfigId=:configId " +
                "AND extensionEpgUrl=:epgUrl"
    )
    abstract fun deleteProgramByConfig(
        configId: String,
        epgUrl: String
    ): Completable

}