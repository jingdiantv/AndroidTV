package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
abstract class ExtensionsChannelDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<ExtensionsChannel>): Completable

    @Update
    abstract fun update(list: List<ExtensionsChannel>): Completable

    @Delete
    abstract fun delete(channel: ExtensionsChannel): Completable

    @Delete
    abstract fun delete(listChannel: List<ExtensionsChannel>): Completable

    @Transaction
    @Query("DELETE FROM extensionschannel WHERE extensionSourceId=:sourceId")
    abstract fun deleteBySourceId(sourceId: String): Completable

    @Transaction
    @Query("SELECT * FROM extensionschannel WHERE extensionSourceId=:sourceId")
    abstract fun getAllBySourceId(sourceId: String): Single<List<ExtensionsChannel>>

    @Transaction
    @Query(
        "SELECT * FROM extensionschannel WHERE extensionSourceId=:sourceId " +
                "AND tvGroup=:category " +
                "LIMIT :limit " +
                "OFFSET :offset "
    )
    abstract fun getAllBySourceIdAndGroup(
        sourceId: String,
        category: String,
        limit: Int,
        offset: Int,
    ): Single<List<ExtensionsChannel>>

}