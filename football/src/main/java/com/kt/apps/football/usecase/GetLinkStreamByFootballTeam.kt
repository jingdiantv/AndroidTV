package com.kt.apps.football.usecase

import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.football.model.FootballDataSourceFrom
import com.kt.apps.football.model.FootballMatchWithStreamLink
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class GetLinkStreamByFootballTeam @Inject constructor(
    private val getListFootballMatch: GetListFootballMatch,
    private val getLinkStreamForFootballMatch: GetLinkStreamForFootballMatch
) : BaseUseCase<FootballMatchWithStreamLink>() {
    override fun prepareExecute(params: Map<String, Any>): Observable<FootballMatchWithStreamLink> {
        val teamName: String = (params[EXTRA_TEAM_NAME] as String)
        val teamArr = teamName.lowercase().split(" ")
        val listMatch = getListFootballMatch(FootballDataSourceFrom.Phut91)
            .blockingFirst()

        var match = listMatch.firstOrNull {
            val matchName = it.getMatchName().lowercase()
            var count = teamArr.size
            teamArr.forEach { t ->
                if (matchName.contains(t)) {
                    count--
                }
            }
            count == 0
        }

        if (match == null) {
            match = listMatch.firstOrNull {
                val league = it.league.lowercase()
                var count = teamArr.size
                teamArr.forEach { t ->
                    if (league.contains(t)) {
                        count--
                    }
                }
                count == 0
            }
        }
        match ?: return Observable.error(Throwable("No available match for team: $teamName"))
        return getLinkStreamForFootballMatch(match)
    }

    operator fun invoke(teamName: String): Observable<FootballMatchWithStreamLink> {
        return execute(
            mapOf(EXTRA_TEAM_NAME to teamName)
        )
    }

    companion object {
        private const val EXTRA_TEAM_NAME = "extra:team_name"
    }


}