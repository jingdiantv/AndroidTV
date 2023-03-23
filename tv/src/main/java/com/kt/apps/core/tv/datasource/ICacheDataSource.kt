package com.kt.apps.core.tv.datasource

import io.reactivex.rxjava3.core.Observable

interface ICacheDataSource<T> {
    fun getLastWatchedMedia(): Observable<List<T>>
    fun recommendMedia(): Observable<List<T>>

}