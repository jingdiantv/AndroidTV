package com.kt.apps.core.tv.di

import com.kt.apps.core.tv.model.TVDataSourceFrom
import dagger.MapKey

@MapKey
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class TVDataSourceMapKey(
    val value: TVDataSourceFrom
) {
}