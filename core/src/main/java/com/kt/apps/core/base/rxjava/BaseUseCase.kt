package com.kt.apps.core.base.rxjava

import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable

abstract class BaseUseCase<T : Any>(private val transformer: AsyncTransformer<T> = AsyncTransformer()) {
    var cacheData: T? = null
    abstract fun prepareExecute(params: Map<String, Any>): Observable<T>
    fun execute(params: Map<String, Any>): Observable<T> {
        return prepareExecute(params)
            .compose(transformer)
    }

    fun error(message: String): Observable<T> = Observable.error(Throwable(message))
}

abstract class MaybeUseCase<T: Any>(private val transformer: MaybeAsyncTransformer<T> = MaybeAsyncTransformer()) {
    abstract fun prepareExecute(params: Map<String, Any>): Maybe<T>
    fun execute(params: Map<String, Any>): Maybe<T> {
        return prepareExecute(params)
            .compose(transformer)
    }

    fun error(message: String): Observable<T> = Observable.error(Throwable(message))
}