package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ExtensionsConfigWithLoadedListChannel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
abstract class ExtensionsConfigDAO {

    @Query("SELECT * FROM ExtensionsConfig")
    @Transaction
    abstract fun getAll(): Observable<List<ExtensionsConfig>>


    @Query("SELECT * FROM ExtensionsConfig WHERE sourceName = :name")
    @Transaction
    abstract fun getExtensions(name: String): Observable<ExtensionsConfig>

    @Query("SELECT * FROM ExtensionsConfig WHERE sourceUrl = :id")
    @Transaction
    abstract fun getExtensionById(id: String): Observable<ExtensionsConfig>

    @Query("SELECT * FROM ExtensionsConfig WHERE sourceUrl = :id")
    abstract fun getExtensionChannelList(id: String): Single<ExtensionsConfigWithLoadedListChannel>

    @Delete
    abstract fun delete(config: ExtensionsConfig): Completable

    @Delete
    abstract fun delete(configs: List<ExtensionsConfig>): Completable

    @Query("DELETE FROM ExtensionsConfig")
    abstract fun deleteAll(): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(config: ExtensionsConfig): Completable
}