package com.kt.apps.football.model

data class League(
    val name: String,
    val country: String,
    val id: String,
    val priority: Int = 0
) {
    var rankingUrl: String? = null
}