package com.kt.apps.football.usecase

import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.football.datasource.IFootballMatchDataSource
import com.kt.apps.football.model.FootballDataSourceFrom
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.football.model.FootballMatchWithStreamLink
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class GetLinkStreamForFootballMatch @Inject constructor(
    private val sourceIterator: Map<FootballDataSourceFrom, @JvmSuppressWildcards IFootballMatchDataSource>
) : BaseUseCase<FootballMatchWithStreamLink>() {
    override fun prepareExecute(params: Map<String, Any>): Observable<FootballMatchWithStreamLink> {
        val repo = sourceIterator[params[EXTRA_SOURCE_FROM] as FootballDataSourceFrom]!!
        val match = params[EXTRA_MATCH] as FootballMatch
        return repo.getLinkLiveStream(match)
    }

    operator fun invoke(match: FootballMatch, sourceFrom: FootballDataSourceFrom) = execute(
        mapOf(
            EXTRA_SOURCE_FROM to sourceFrom,
            EXTRA_MATCH to match
        )
    )

    operator fun invoke(match: FootballMatch, sourceFrom: FootballDataSourceFrom, html: String) = execute(
        mapOf(
            EXTRA_SOURCE_FROM to sourceFrom,
            EXTRA_MATCH to match,
            EXTRA_HTML to html
        )
    )

    operator fun invoke(match: FootballMatch) = invoke(match, match.sourceFrom)
    operator fun invoke(match: FootballMatch, html: String) = invoke(match, match.sourceFrom, html)

    companion object {
        private const val EXTRA_SOURCE_FROM = "extra:source_from"
        private const val EXTRA_MATCH = "extra:match"
        private const val EXTRA_HTML = "extra:html_page"
    }
}