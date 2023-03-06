package com.kt.apps.football.datasource

import com.kt.apps.football.model.FootballMatch
import com.kt.apps.football.model.FootballMatchWithStreamLink
import io.reactivex.rxjava3.core.Observable

interface IFootballMatchDataSource {
    fun getAllMatches(): Observable<List<FootballMatch>>
    fun getLinkLiveStream(match: FootballMatch): Observable<FootballMatchWithStreamLink>
}