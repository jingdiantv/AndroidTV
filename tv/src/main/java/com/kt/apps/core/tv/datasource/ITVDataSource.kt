package com.kt.apps.core.tv.datasource

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.storage.TVStorage
import io.reactivex.rxjava3.core.Observable

interface ITVDataSource {
    fun getTvList(): Observable<List<TVChannel>>
    fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean = false
    ): Observable<TVChannelLinkStream>
}

const val EXTRA_KEY_VERSION_NEED_REFRESH = "version_need_refresh"
const val EXTRA_KEY_USE_ONLINE = "use_online_data"

fun ITVDataSource.needRefreshData(
    remoteConfig: FirebaseRemoteConfig,
    tvStorage: TVStorage
): Boolean {
    remoteConfig.fetchAndActivate()
    val needRefresh = remoteConfig.getBoolean(EXTRA_KEY_USE_ONLINE)
    val version = remoteConfig.getLong(EXTRA_KEY_VERSION_NEED_REFRESH)
    val refreshedInVersion = tvStorage.getVersionRefreshed(EXTRA_KEY_VERSION_NEED_REFRESH)
    Logger.d(
        this, message = "{" +
                "useOnlineData: $needRefresh, " +
                "version: $version, " +
                "refreshedVersion: $refreshedInVersion" +
                "}"
    )
    return needRefresh && version > refreshedInVersion
}
