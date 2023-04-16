package com.kt.apps.core.tv.datasource.impl

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.kt.apps.core.Constants
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.tv.datasource.ITVDataSource
import com.kt.apps.core.tv.datasource.needRefreshData
import com.kt.apps.core.tv.di.TVScope
import com.kt.apps.core.tv.model.*
import com.kt.apps.core.tv.storage.TVStorage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Provider

@TVScope
class MainTVDataSource @Inject constructor(
    private val sctvDataSource: SCTVDataSourceImpl,
    private val vDataSourceImpl: VDataSourceImpl,
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val tvStorage: Provider<TVStorage>,
    private val roomDataBase: RoomDataBase
) : ITVDataSource {
    private val compositeDisposable by lazy {
        CompositeDisposable()
    }

    private val needRefresh: Boolean
        get() = this.needRefreshData(firebaseRemoteConfig, tvStorage.get())

    private val versionNeedRefresh: Long
        get() = firebaseRemoteConfig.getLong(Constants.EXTRA_KEY_VERSION_NEED_REFRESH)


    override fun getTvList(): Observable<List<TVChannel>> {
        val dataBaseSource = roomDataBase.tvChannelDao()
            .getListChannelWithUrl()
            .flatMapObservable {
                if (it.isEmpty()) {
                    getFirebaseSource()
                } else {
                    Logger.d(this@MainTVDataSource, message = "Offline source: ${Gson().toJson(it)}")
                    Observable.just(
                        it.map { tvChannelFromDB ->
                            TVChannel(
                                tvGroup = tvChannelFromDB.tvChannel.tvGroup,
                                tvChannelName = tvChannelFromDB.tvChannel.tvChannelName,
                                tvChannelWebDetailPage = tvChannelFromDB.urls.firstOrNull {
                                    it.type == TVChannelUrlType.WEB_PAGE.value
                                }?.url ?: tvChannelFromDB.urls[0].url,
                                urls = tvChannelFromDB.urls.map { url ->
                                    TVChannel.Url(
                                        dataSource = url.src,
                                        url = url.url,
                                        type = url.type
                                    )
                                },
                                sourceFrom = TVDataSourceFrom.MAIN_SOURCE.name,
                                logoChannel = tvChannelFromDB.tvChannel.logoChannel,
                                channelId = tvChannelFromDB.tvChannel.channelId
                            )
                        }
                    )
                }
            }

        val onlineSource = getFirebaseSource()
        if (!needRefresh) {
            return dataBaseSource
                .onErrorResumeNext {
                    Logger.e(this@MainTVDataSource, exception = it)
                    if (it.message == "EmptyData") {
                        onlineSource
                    } else {
                        Observable.error(it)
                    }
                }
        }

        return onlineSource
    }

    private fun getFirebaseSource(): Observable<List<TVChannel>> =
        Observable.create<List<TVChannel>> { emitter ->
            val totalGroup = supportGroups.size
            val totalList = mutableListOf<TVChannel>()
            var totalCount = 0
            Logger.d(this@MainTVDataSource, message = "getFirebaseSource")

            supportGroups.forEach { group ->
                firebaseDatabase.reference
                    .child(ALL_CHANNEL_NAME)
                    .ref
                    .child(group.name)
                    .get()
                    .addOnSuccessListener {
                        val value = it.getValue<List<TVChannelFromDB?>>() ?: return@addOnSuccessListener
                        val tvList = value.filterNotNull()
                            .map { tvChannelFromDB ->
                                val totalUrls = tvChannelFromDB.urls.filterNotNull()
                                TVChannel(
                                    tvGroup = tvChannelFromDB.group,
                                    tvChannelName = tvChannelFromDB.name,
                                    tvChannelWebDetailPage = totalUrls.firstOrNull {
                                        it.type == TVChannelUrlType.WEB_PAGE.value
                                    }?.url ?: totalUrls[0].url,
                                    urls = totalUrls
                                        .map { url ->
                                            TVChannel.Url(
                                                dataSource = url.src,
                                                url = url.url,
                                                type = url.type
                                            )
                                        },
                                    sourceFrom = TVDataSourceFrom.MAIN_SOURCE.name,
                                    logoChannel = tvChannelFromDB.thumb,
                                    channelId = tvChannelFromDB.id
                                )
                            }

                        totalList.addAll(tvList)
                        emitter.onNext(tvList)
                        saveToRoomDB(tvList)
                        totalCount++
                        Logger.d(this@MainTVDataSource, message = "Counter: $totalCount, $totalGroup")
                        if (totalCount == totalGroup) {
                            emitter.onComplete()
                            tvStorage.get().saveRefreshInVersion(
                                Constants.EXTRA_KEY_VERSION_NEED_REFRESH,
                                versionNeedRefresh
                            )
                        }
                    }
                    .addOnFailureListener {
                        totalCount++
                        if (totalCount == totalGroup && totalList.isEmpty()) {
                            emitter.onNext(totalList)
                            emitter.onComplete()
                        } else {
                            emitter.onError(it)
                        }
                    }

            }
        }

    private fun saveToRoomDB(tvDetails: List<TVChannel>) {
        val tvUrl = mutableListOf<TVChannelDTO.TVChannelUrl>()
        val tvChannelList = mutableListOf<TVChannelDTO>()
        tvDetails.forEach { channel ->

            channel.urls.forEach {
                tvUrl.add(
                    TVChannelDTO.TVChannelUrl(
                        src = it.dataSource,
                        type = it.type,
                        url = it.url,
                        tvChannelId = channel.channelId
                    )
                )
            }

            tvChannelList.add(
                TVChannelDTO(
                    channel.tvGroup,
                    channel.logoChannel,
                    channel.tvChannelName,
                    sourceFrom = TVDataSourceFrom.MAIN_SOURCE.name,
                    channel.channelId
                )
            )
        }

        val source1 = roomDataBase.tvChannelDao()
            .insertListChannel(
                tvChannelList
            )

        val source2 = roomDataBase.tvChannelUrlDao()
            .insert(tvUrl)


        compositeDisposable.add(
            Completable.concatArray(source1, source2)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Logger.d(this@MainTVDataSource, message = "Insert source success")
                }, {
                    Logger.e(this@MainTVDataSource, exception = it)
                })
        )
    }

    override fun getTvLinkFromDetail(
        tvChannel: TVChannel,
        isBackup: Boolean
    ): Observable<TVChannelLinkStream> {
        Logger.d(this@MainTVDataSource, message = "getTVFromDetail: ${Gson().toJson(tvChannel)}")

        val streamingUrl = tvChannel.urls
            .filter {
                it.url.isNotBlank()
            }
            .filter {
                it.type.lowercase() == TVChannelUrlType.STREAM.value
            }
            .map {
                if (it.url.contains("|Referer")) {
                    val refererIndex = it.url.indexOf("|Referer")
                    val newUrl = it.url.substring(0, refererIndex)
                    it.url = newUrl
                    it
                } else {
                    it
                }
            }

        tvChannel.urls
            .filter {
                it.url.isNotBlank()
            }
            .firstOrNull {
                it.type.lowercase() == TVChannelUrlType.WEB_PAGE.value
            }?.let {
                return when (it.dataSource) {

                    TVChannelURLSrc.SCTV.value -> {
                        sctvDataSource.getTvLinkFromDetail(tvChannel, isBackup)
                    }

                    TVChannelURLSrc.V.value -> {
                        tvChannel.tvChannelWebDetailPage = it.url
                        vDataSourceImpl.getTvLinkFromDetail(tvChannel, isBackup)
                            .onErrorResumeNext {
                                Logger.e(this@MainTVDataSource, "VDataSource", it)
                                Observable.just(
                                    TVChannelLinkStream(
                                        tvChannel,
                                        streamingUrl.map {
                                            it.url
                                        }
                                    )
                                )
                            }
                    }

                    else -> {
                        vDataSourceImpl.getTvLinkFromDetail(tvChannel, isBackup)
                    }
                }.map { finalTVWithLinkStream ->
                    if (streamingUrl.isNotEmpty()) {
                        val newListStreaming = finalTVWithLinkStream.linkStream.toMutableList()
                        newListStreaming.addAll(streamingUrl.map { it.url })
                        return@map TVChannelLinkStream(
                            finalTVWithLinkStream.channel,
                            newListStreaming.distinct()
                        )
                    }
                    return@map finalTVWithLinkStream
                }
            }

        return Observable.just(
            TVChannelLinkStream(
                tvChannel,
                streamingUrl.map { it.url }
            )
        )
    }

    data class TVChannelFromDB @JvmOverloads constructor(
        var group: String = "",
        var id: String = "",
        var isRadio: Boolean? = false,
        var name: String = "",
        var thumb: String = "",
        var urls: List<Url?> = listOf()
    ) {
        data class Url @JvmOverloads constructor(
            val src: String? = null,
            val type: String = "",
            val url: String = ""
        )
    }

    enum class TVChannelUrlType(val value: String) {
        STREAM("streaming"), WEB_PAGE("web")
    }

    enum class TVChannelURLSrc(val value: String) {
        V("vieon"), SCTV("sctv")
    }

    companion object {
        private const val ALL_CHANNEL_NAME = "AllChannels"
        private val supportGroups by lazy {
            listOf(
                TVChannelGroup.VTV,
                TVChannelGroup.HTV,
                TVChannelGroup.SCTV,
                TVChannelGroup.VTC,
                TVChannelGroup.THVL,
                TVChannelGroup.AnNinh,
                TVChannelGroup.HTVC,
                TVChannelGroup.DiaPhuong,
                TVChannelGroup.Intenational,
                TVChannelGroup.Kid,

                TVChannelGroup.VOV,
                TVChannelGroup.VOH,
            )
        }
    }
}