package com.kt.apps.core.tv.datasource.impl

import androidx.core.os.bundleOf
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.MapChannel
import com.kt.apps.core.tv.FirebaseLogUtils
import com.kt.apps.core.tv.datasource.ITVDataSource
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

class VDataSourceImpl @Inject constructor(
    private val compositeDisposable: DisposableContainer,
    private val keyValueStorage: TVStorage,
    private val roomDataBase: RoomDataBase,
    private val remoteConfig: FirebaseRemoteConfig,
    private val firebaseDatabase: FirebaseDatabase
) : ITVDataSource {

    private fun needRefresh(name: String): Boolean {
        val needRefresh = remoteConfig.getBoolean(name)
        val version = remoteConfig.getLong("${name}_refresh")
        val refreshedInVersion = keyValueStorage.getVersionRefreshed(name)
        return needRefresh && version > refreshedInVersion
    }

    private var isOnline: Boolean = false

    override fun getTvList(): Observable<List<TVChannel>> {
        val listGroup = TVChannelGroup.values().map {
            it.name
        }

        return Observable.create<List<TVChannel>> { emitter ->
            val totalChannel = mutableListOf<TVChannel>()
            var count = 0
            listGroup.forEach { group ->
                val needRefresh = true
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
                        if (needRefresh) {
                            keyValueStorage.saveRefreshInVersion(
                                group,
                                remoteConfig.getLong("${group}_refresh")
                            )
                        }
                        keyValueStorage.saveTVByGroup(group, it)
                        saveToRoomDB(group, it)
                        totalChannel.addAll(it)
                        count++
                        if (count == listGroup.size) {
                            emitter.onNext(totalChannel)
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
            val body = Jsoup.connect(tvChannel.tvChannelWebDetailPage)
                .header("referer", tvChannel.tvChannelWebDetailPage)
                .header("origin", tvChannel.tvChannelWebDetailPage.getBaseUrl())
                .execute()
                .parse()
                .body()
            if (emitter.isDisposed) {
                return@create
            }
            body.getElementById("__NEXT_DATA__")?.let {
                val text = it.html()
                val jsonObject = JSONObject(text)
                val linkM3u8 = jsonObject.getJSONObject("props")
                    .getJSONObject("initialState")
                    .getJSONObject("LiveTV")
                    .getJSONObject("detailChannel")
                    .optString("linkPlayHls")
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
                val value = it.getValue<List<DataFromFirebase>>() ?: return@addOnSuccessListener
                onComplete(
                    value.map { dataFromFirebase ->
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