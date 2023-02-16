package com.kt.apps.core.base

import io.reactivex.rxjava3.core.Observable

sealed class DataState<T> {
    open class Success<T>(val data: T) : DataState<T>()
    open class Next<T>(val data: T) : DataState<T>()
    open class Error<T>(val throwable: Throwable) : DataState<T>()
    open class Loading<T>() : DataState<T>()
    class None<T> : DataState<T>()
}

fun <T> DataState.Success<T>.mapToObserver(): Observable<DataState<T>> = Observable.just(this)
fun <T> DataState.Next<T>.mapToObserver(): Observable<DataState.Next<T>> = Observable.just(this)