package com.kt.apps.core.tv.datasource

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.storage.TVStorage
import io.reactivex.rxjava3.core.Observable
import java.util.regex.Pattern

interface ITVDataSource {
    fun getTvList(): Observable<List<TVChannel>>
    fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean = false
    ): Observable<TVChannelLinkStream>

    companion object {
        fun getPriority(group: String): Int {
            return when (group.lowercase()) {
                TVChannelGroup.VTV.name.lowercase() -> 100
                TVChannelGroup.VTC.name.lowercase() -> 200
                TVChannelGroup.HTV.name.lowercase() -> 300
                TVChannelGroup.HTVC.name.lowercase() -> 400
                TVChannelGroup.SCTV.name.lowercase() -> 500
                TVChannelGroup.THVL.name.lowercase() -> 600
                TVChannelGroup.Intenational.name.lowercase() -> 700
                TVChannelGroup.Kid.name.lowercase() -> 800
                else -> 1000
            }
        }
        fun sortTVChannel(): (TVChannel) -> Int = {
            val priority = getPriority(it.tvGroup)
            if (priority < 1000) {
                val matcher = Pattern.compile("[0-9]+")
                    .matcher(it.tvChannelName)
                var num = 99
                while (matcher.find()) {
                    num = matcher.group(0)?.toInt() ?: 99
                }

                priority + num
            } else {
                priority
            }
        }
    }
}

fun ITVDataSource.needRefreshData(
    remoteConfig: FirebaseRemoteConfig,
    tvStorage: TVStorage
): Boolean {
    remoteConfig.fetchAndActivate()
    val needRefresh = remoteConfig.getBoolean(Constants.EXTRA_KEY_USE_ONLINE)
    val version = remoteConfig.getLong(Constants.EXTRA_KEY_VERSION_NEED_REFRESH)
    val refreshedInVersion = tvStorage.getVersionRefreshed(Constants.EXTRA_KEY_VERSION_NEED_REFRESH)
    Logger.d(
        this, message = "{" +
                "useOnlineData: $needRefresh, " +
                "version: $version, " +
                "refreshedVersion: $refreshedInVersion" +
                "}"
    )
    return needRefresh && version > refreshedInVersion
}
