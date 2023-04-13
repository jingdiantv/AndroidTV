package com.kt.apps.core.tv.datasource.impl

import androidx.core.os.bundleOf
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.MapChannel
import com.kt.apps.core.tv.FirebaseLogUtils
import com.kt.apps.core.tv.datasource.EXTRA_KEY_VERSION_NEED_REFRESH
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.datasource.needRefreshData
import com.kt.apps.core.tv.di.TVScope
import com.kt.apps.core.tv.model.*
import com.kt.apps.core.tv.storage.TVStorage
import com.kt.apps.core.utils.getBaseUrl
import com.kt.apps.core.utils.removeAllSpecialChars
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.DisposableContainer
import io.reactivex.rxjava3.schedulers.Schedulers
import org.json.JSONObject
import org.jsoup.Jsoup
import javax.inject.Inject

@TVScope
class VDataSourceImpl @Inject constructor(
    private val compositeDisposable: DisposableContainer,
    private val keyValueStorage: TVStorage,
    private val roomDataBase: RoomDataBase,
    private val remoteConfig: FirebaseRemoteConfig,
    private val firebaseDatabase: FirebaseDatabase
) : ITVDataSource {
    private fun getSupportTVGroup(): List<TVChannelGroup> {
        return listOf(
            TVChannelGroup.VTV,
            TVChannelGroup.HTV,
            TVChannelGroup.VTC,
            TVChannelGroup.HTVC,
            TVChannelGroup.THVL,
            TVChannelGroup.DiaPhuong,
            TVChannelGroup.AnNinh,
            TVChannelGroup.VOV,
            TVChannelGroup.VOH,
            TVChannelGroup.Intenational
        )
    }

    private val _needRefresh: Boolean
        get() = this.needRefreshData(remoteConfig, keyValueStorage)

    private var isOnline: Boolean = false

    override fun getTvList(): Observable<List<TVChannel>> {
        val listGroup = getSupportTVGroup().map {
            it.name
        }

        return Observable.create<List<TVChannel>> { emitter ->
            val totalChannel = mutableListOf<TVChannel>()
            var count = 0
            val needRefresh = _needRefresh
            listGroup.forEach { group ->
                if (keyValueStorage.getTvByGroup(group).isNotEmpty() && !needRefresh) {
                    isOnline = false
                    totalChannel.addAll(keyValueStorage.getTvByGroup(group))
                    saveToRoomDB(group, keyValueStorage.getTvByGroup(group))
                    count++
                    if (count == listGroup.size) {
                        emitter.onNext(totalChannel)
                        emitter.onComplete()
                    }
                } else {
                    isOnline = true
                    fetchTvList(group) {
                        keyValueStorage.saveTVByGroup(group, it)
                        saveToRoomDB(group, it)
                        totalChannel.addAll(it)
                        count++
                        if (count == listGroup.size) {
                            emitter.onNext(totalChannel)
                            if (needRefresh) {
                                keyValueStorage.saveRefreshInVersion(
                                    EXTRA_KEY_VERSION_NEED_REFRESH,
                                    remoteConfig.getLong(EXTRA_KEY_VERSION_NEED_REFRESH)
                                )
                            }
                            emitter.onComplete()
                        }
                    }.addOnFailureListener {
                        count++
                        emitter.onError(it)
                    }
                }
            }

        }.doOnError {
            FirebaseLogUtils.logGetListChannelError(TVDataSourceFrom.V.name, it)
        }.doOnComplete {
            FirebaseLogUtils.logGetListChannel(
                TVDataSourceFrom.V.name,
                bundleOf("fetch_from" to if (isOnline) "online" else "offline")
            )
        }
    }

    private fun saveToRoomDB(source: String, tvDetails: List<TVChannel>) {
        compositeDisposable.add(
            roomDataBase.mapChannelDao()
                .insert(
                    tvDetails.map {
                        val id = it.tvChannelWebDetailPage
                            .trim()
                            .removeSuffix("/")
                            .split("/")
                            .last()
                        MapChannel(
                            channelId = id,
                            channelName = it.tvChannelName,
                            fromSource = TVDataSourceFrom.V.name,
                            channelGroup = source
                        )
                    }
                )
                .subscribeOn(Schedulers.io())
                .subscribe({
                }, {
                })
        )
    }

    override fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean
    ): Observable<TVChannelLinkStream> {
        return Observable.create { emitter ->
            Logger.d(this@VDataSourceImpl, message = "getTvLinkFromDetail")
            val body = try {
                Jsoup.connect(tvChannel.tvChannelWebDetailPage)
                    .header("referer", tvChannel.tvChannelWebDetailPage)
                    .header("origin", tvChannel.tvChannelWebDetailPage.getBaseUrl())
                    .execute()
                    .parse()
                    .body()
            } catch (e: java.lang.Exception) {
                emitter.onError(e)
                return@create
            }
            if (emitter.isDisposed) {
                return@create
            }
            body.getElementById("__NEXT_DATA__")?.let {
                val text = it.html()
                val jsonObject = JSONObject(text)
                val linkM3u8 = try {
                    jsonObject.getJSONObject("props")
                        .getJSONObject("initialState")
                        .getJSONObject("LiveTV")
                        .getJSONObject("detailChannel")
                        .optString("linkPlayHls")
                } catch (e: Exception) {
                    emitter.onError(e)
                    return@create
                }
                if (emitter.isDisposed) {
                    return@create
                }
                emitter.onNext(
                    TVChannelLinkStream(
                        tvChannel,
                        listOf(linkM3u8)
                    )
                )
            }
            if (emitter.isDisposed) {
                return@create
            }
            emitter.onComplete()
        }
    }

    private fun fetchTvList(
        name: String,
        onComplete: (list: List<TVChannel>) -> Unit
    ): Task<DataSnapshot> {
        return firebaseDatabase.reference.child(name)
            .get()
            .addOnSuccessListener {
                val value = it.getValue<List<DataFromFirebase?>>() ?: return@addOnSuccessListener
                onComplete(
                    value.filterNotNull().map { dataFromFirebase ->
                        TVChannel(
                            name,
                            tvChannelName = dataFromFirebase.name,
                            tvChannelWebDetailPage = dataFromFirebase.url,
                            logoChannel = dataFromFirebase.logo,
                            sourceFrom = TVDataSourceFrom.V.name,
                            channelId = if (name in listOf(TVChannelGroup.VOV.name, TVChannelGroup.VOH.name)) {
                                dataFromFirebase.name.removeAllSpecialChars()
                            } else {
                                dataFromFirebase.url.trim().removeSuffix("/").split("/").last()
                            }
                        )
                    })
            }
    }

}