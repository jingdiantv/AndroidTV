package com.kt.apps.core.tv.usecase

import com.kt.apps.core.base.rxjava.BaseUseCase
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVDataSourceFrom
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class GetListTVChannel @Inject constructor(
    private val tvDataSources: Map<TVDataSourceFrom, @JvmSuppressWildcards ITVDataSource>,
) : BaseUseCase<List<TVChannel>>() {

    override fun prepareExecute(params: Map<String, Any>): Observable<List<TVChannel>> {
        if (cacheData != null && (params[EXTRA_REFRESH_DATA] as Boolean)) {
            return Observable.just(cacheData!!)
        }

        return (params[EXTRA_TV_SOURCE_FROM]?.let { source ->
            when (source as TVDataSourceFrom) {
                TVDataSourceFrom.V -> return (tvDataSources[source]!!.getTvList())
                    .onErrorResumeNext {
                        Observable.concat(
                            tvDataSources[TVDataSourceFrom.VTV_BACKUP]?.getTvList()
                                ?: error("Null data sources VTV_BACKUP provider"),
                            tvDataSources[TVDataSourceFrom.VTC_BACKUP]?.getTvList() ?: error(""),
                            tvDataSources[TVDataSourceFrom.HTV_BACKUP]?.getTvList() ?: error(""),
                        )
                    }
                else -> {
                    tvDataSources[source]?.getTvList() ?: Observable.error(Throwable(""))
                }
            }
        } ?: Observable.concat(
            tvDataSources[TVDataSourceFrom.VTV_BACKUP]?.getTvList() ?: error("Null data sources VTV_BACKUP provider"),
            tvDataSources[TVDataSourceFrom.VTC_BACKUP]?.getTvList() ?: error(""),
            tvDataSources[TVDataSourceFrom.HTV_BACKUP]?.getTvList() ?: error(""),
        ))
    }

    operator fun invoke(
        forceRefreshData: Boolean = false,
        sourceFrom: TVDataSourceFrom = TVDataSourceFrom.V,
    ): Observable<List<TVChannel>> {
        return execute(
            mapOf(
                EXTRA_TV_SOURCE_FROM to sourceFrom,
                EXTRA_REFRESH_DATA to forceRefreshData
            )
        )
    }

    companion object {
        private const val EXTRA_TV_SOURCE_FROM = "extra:tv_source_from"
        private const val EXTRA_REFRESH_DATA = "extra:refresh_data"
    }
}