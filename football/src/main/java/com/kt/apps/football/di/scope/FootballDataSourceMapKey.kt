package com.kt.apps.football.di.scope

import com.kt.apps.football.model.FootballDataSourceFrom
import dagger.MapKey

@MapKey
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class FootballDataSourceMapKey(
    val key: FootballDataSourceFrom
) {
}