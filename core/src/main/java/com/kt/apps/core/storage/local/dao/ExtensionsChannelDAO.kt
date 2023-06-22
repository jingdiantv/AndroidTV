package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kt.apps.core.extensions.ExtensionsChannel
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
    @Query(
        "SELECT * FROM extensionschannel WHERE extensionSourceId=:sourceId " +
                "ORDER BY CASE " +
                "WHEN LENGTH(tvChannelName) < 2 THEN tvChannelName " +
                "WHEN tvChannelName LIKE 'A%' " +
                "OR tvChannelName LIKE 'Á%' " +
                "OR tvChannelName LIKE 'À%' " +
                "OR tvChannelName LIKE 'Ả%' " +
                "OR tvChannelName LIKE 'Ã%' " +
                "OR tvChannelName LIKE 'Ạ%' THEN 'A'||substr(tvChannelName, 1, 3)" +
                "WHEN tvChannelName LIKE 'Â%' " +
                "OR tvChannelName LIKE 'Ấ%' " +
                "OR tvChannelName LIKE 'Ầ%' " +
                "OR tvChannelName LIKE 'Ẩ%' " +
                "OR tvChannelName LIKE 'Ẫ%' " +
                "OR tvChannelName LIKE 'Ậ%' THEN 'Azz'" +
                "WHEN tvChannelName LIKE 'Ă%' " +
                "OR tvChannelName LIKE 'Ắ%' " +
                "OR tvChannelName LIKE 'Ằ%' " +
                "OR tvChannelName LIKE 'Ẳ%' " +
                "OR tvChannelName LIKE 'Ẵ%' " +
                "OR tvChannelName LIKE 'Ặ%' THEN 'Az'||substr(tvChannelName, 1 , 2)" +
                "WHEN tvChannelName LIKE 'Đ%' THEN 'Dz'||substr(tvChannelName, 1 , 2)" +
                "WHEN tvChannelName LIKE 'Ê%' " +
                "OR tvChannelName LIKE 'Ế%' " +
                "OR tvChannelName LIKE 'Ề%' " +
                "OR tvChannelName LIKE 'Ể%' " +
                "OR tvChannelName LIKE 'Ễ%' " +
                "OR tvChannelName LIKE 'Ệ%' " +
                "THEN 'Ez'||substr(tvChannelName, 1 , 2) " +
                "WHEN tvChannelName LIKE 'Ô%' " +
                "OR tvChannelName LIKE 'Ố%' " +
                "OR tvChannelName LIKE 'Ồ%' " +
                "OR tvChannelName LIKE 'Ổ%' " +
                "OR tvChannelName LIKE 'Ỗ%' " +
                "OR tvChannelName LIKE 'Ộ%' " +
                "THEN 'Oz'||substr(tvChannelName, 1 , 2)" +
                "WHEN tvChannelName LIKE 'Ơ%' " +
                "OR tvChannelName LIKE 'Ớ%' " +
                "OR tvChannelName LIKE 'Ờ%' " +
                "OR tvChannelName LIKE 'Ở%' " +
                "OR tvChannelName LIKE 'Ỡ%' " +
                "OR tvChannelName LIKE 'Ợ%' " +
                "THEN 'Ozz'" +
                "WHEN tvChannelName LIKE 'Ư%' " +
                "OR tvChannelName LIKE 'Ứ%' " +
                "OR tvChannelName LIKE 'Ừ%' " +
                "OR tvChannelName LIKE 'Ử%' " +
                "OR tvChannelName LIKE 'Ữ%' " +
                "OR tvChannelName LIKE 'Ự%' " +
                "THEN 'Uz'||substr(tvChannelName, 1 , 2) " +
                "ELSE substr(tvChannelName, 0, 3)" +
                "END"
    )
    abstract fun getAllBySourceId(sourceId: String): Single<List<ExtensionsChannel>>

    @Transaction
    @Query(
        "SELECT * FROM extensionschannel WHERE extensionSourceId=:sourceId " +
                "ORDER BY tvChannelName " +
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