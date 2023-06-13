package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kt.apps.core.storage.local.dto.ExtensionChannelCategory
import com.kt.apps.core.storage.local.dto.ExtensionsCategoryWithListChannel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
abstract class ExtensionsChannelCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(list: List<ExtensionChannelCategory>): Completable

    @Update
    abstract fun update(list: List<ExtensionChannelCategory>): Completable

    @Delete
    abstract fun delete(channel: ExtensionChannelCategory): Completable

    @Delete
    abstract fun delete(listChannel: List<ExtensionChannelCategory>): Completable

    @Transaction
    @Query("DELETE FROM ExtensionChannelCategory WHERE configSourceUrl=:sourceId")
    abstract fun deleteBySourceId(sourceId: String): Completable

    @Transaction
    @Query("SELECT * FROM ExtensionChannelCategory WHERE configSourceUrl=:sourceId")
    abstract fun getAllBySourceId(sourceId: String): Single<List<ExtensionsCategoryWithListChannel>>

    @Transaction
    @Query(
        "SELECT * FROM ExtensionChannelCategory " +
                "WHERE configSourceUrl=:sourceId " +
                "AND name=:name LIMIT 1"
    )
    abstract fun getAllBySourceIdAndName(
        sourceId: String,
        name: String
    ): Single<ExtensionsCategoryWithListChannel>
}