package com.kt.apps.core.base.provider

import com.kt.apps.core.storage.local.dto.TVChannelEntity

abstract class RecommendationProvider<T>() {
    abstract fun sendRecommendation(item: T)
}
