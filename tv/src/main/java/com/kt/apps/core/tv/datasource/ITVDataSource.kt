package com.kt.apps.core.tv.datasource

import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import io.reactivex.rxjava3.core.Observable

interface ITVDataSource {
    fun getTvList(): Observable<List<TVChannel>>
    fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean = false
    ): Observable<TVChannelLinkStream>
}
