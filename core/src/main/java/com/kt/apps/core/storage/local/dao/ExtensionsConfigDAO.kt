package com.kt.apps.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kt.apps.core.extensions.ExtensionsConfig
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

@Dao
abstract class ExtensionsConfigDAO {

    @Query("SELECT * FROM ExtensionsConfig")
    abstract fun getAll(): Observable<List<ExtensionsConfig>>


    @Query("SELECT * FROM ExtensionsConfig WHERE sourceName = :name")
    abstract fun getExtensions(name: String): Observable<ExtensionsConfig>

    @Query("SELECT * FROM ExtensionsConfig WHERE sourceUrl = :id")
    abstract fun getExtensionById(id: String): Observable<ExtensionsConfig>

    @Delete
    abstract fun delete(config: ExtensionsConfig): Completable

    @Delete
    abstract fun delete(configs: List<ExtensionsConfig>): Completable

    @Query("DELETE FROM ExtensionsConfig")
    abstract fun deleteAll(): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(config: ExtensionsConfig): Completable
}