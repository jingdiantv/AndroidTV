package com.kt.apps.core.extensions

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.di.NetworkModule
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.getLastRefreshExtensions
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.ExtensionChannelCategory
import com.kt.apps.core.storage.saveLastRefreshExtensions
import com.kt.apps.core.utils.trustEveryone
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.DisposableContainer
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Named

@CoreScope
class ParserExtensionsSource @Inject constructor(
    private val client: OkHttpClient,
    private val storage: IKeyValueStorage,
    private val roomDataBase: RoomDataBase,
    private val programScheduleParser: ParserExtensionsProgramSchedule,
    @Named(NetworkModule.EXTRA_NETWORK_DISPOSABLE)
    private val disposable: DisposableContainer,
    private val remoteConfig: FirebaseRemoteConfig
) {
    private val extensionsChannelDao by lazy {
        roomDataBase.extensionsChannelDao()
    }

    private val extensionsConfigDao by lazy {
        roomDataBase.extensionsConfig()
    }

    fun getIntervalRefreshData(configType: ExtensionsConfig.Type): Long {
        val key = EXTRA_INTERVAL_REFRESH_DATA_KEY + configType.name
        val defaultValue = when (configType) {
            ExtensionsConfig.Type.TV_CHANNEL -> INTERVAL_REFRESH_DATA_TV_CHANNEL
            ExtensionsConfig.Type.FOOTBALL -> INTERVAL_REFRESH_DATA_FOOTBALL
            ExtensionsConfig.Type.MOVIE -> INTERVAL_REFRESH_DATA_MOVIE
        }

        return storage.get(key, Long::class.java)
            .takeIf {
                it > -1L
            }
            ?: defaultValue.also {
                storage.save(key, it)
            }
    }

    private val pendingSource: MutableMap<String, Maybe<List<ExtensionsChannel>>> by lazy {
        mutableMapOf()
    }


    fun parseFromRemoteMaybe(extension: ExtensionsConfig): Maybe<List<ExtensionsChannel>> {
        return parseFromRemoteRx(extension)
    }

    fun parseFromRemoteRx(extension: ExtensionsConfig): Maybe<List<ExtensionsChannel>> {
        if (pendingSource.containsKey(extension.sourceUrl)) {
            return pendingSource[extension.sourceUrl]!!
        }
        val parserStreamingSource = parseFromRemoteRxStream(extension)
            .doOnSuccess {
                pendingSource.remove(extension.sourceUrl)
            }.doOnError {
                pendingSource.remove(extension.sourceUrl)
            }
            .doOnDispose {
                pendingSource.remove(extension.sourceUrl)
            }
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())

        val onlineSource = if (pendingSource.size <= 5) {
            parserStreamingSource
        } else {
            Completable.create { emitter ->
                try {
                    while (pendingSource.size > 5 && !emitter.isDisposed) {
                        Thread.sleep(100)
                    }
                    if (!emitter.isDisposed) {
                        emitter.onComplete()
                    }
                } catch (e: Exception) {
                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                    }
                }
            }.doOnDispose {
                pendingSource.remove(extension.sourceUrl)
            }.andThen(
                parserStreamingSource
            )
        }.map {
            it as List<ExtensionsChannel>
        }

        val offlineSource = extensionsChannelDao.getAllBySourceId(extension.sourceUrl)
            .toMaybe()
            .flatMap {
                if (it.isEmpty()) {
                    onlineSource
                } else {
                    Maybe.just(it)
                }
            }
            .doOnComplete {
                programScheduleParser.parseForConfig(extension)
            }
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())

        Logger.d(this@ParserExtensionsSource, "execute", "Old time ${extension.sourceUrl}: ${storage.getLastRefreshExtensions(extension)}")

        if (System.currentTimeMillis() - storage.getLastRefreshExtensions(extension) < getIntervalRefreshData(extension.type)) {
            Logger.d(this@ParserExtensionsSource, "execute", "OfflineSource")
            val source = offlineSource
                .onErrorResumeNext {
                    onlineSource
                }
                .doOnComplete {
                    Logger.d(this@ParserExtensionsSource, "execute", "Offline source complete")
                    pendingSource.remove(extension.sourceUrl)
                }
                .doOnError {
                    Logger.d(this@ParserExtensionsSource, "execute", "Offline source error")
                    pendingSource.remove(extension.sourceUrl)
                }
                .doOnDispose {
                    Logger.d(this@ParserExtensionsSource, "execute", "Offline source dispose")
                    pendingSource.remove(extension.sourceUrl)
                }
                .doOnSuccess {
                    Logger.d(this@ParserExtensionsSource, "execute", "Offline source success")
                    pendingSource.remove(extension.sourceUrl)
                }
            pendingSource[extension.sourceUrl] = source
            return source
        }
        Logger.d(this@ParserExtensionsSource, "execute", "OnlineSource")
        pendingSource[extension.sourceUrl] = onlineSource
        return onlineSource
    }

    fun setIntervalRefreshData(time: Long) {
        storage.save(EXTRA_INTERVAL_REFRESH_DATA_KEY, time)
    }

    private fun parseFromRemoteRxStream(extension: ExtensionsConfig): Maybe<MutableList<ExtensionsChannel>> = Maybe.fromCallable {
        trustEveryone()
        val response = client
            .newBuilder()
            .callTimeout(600, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
            .newCall(
                Request.Builder()
                    .url(extension.sourceUrl)
                    .build()

            ).execute()

        if (response.code in 200..299) {
            val stream = response.body.byteStream()
            Logger.d(
                this@ParserExtensionsSource,
                message = "${extension.sourceUrl} - Streaming - Content Length: $${response.body.contentLength()}"
            )
            return@fromCallable stream
        }
        if (response.code >= 500 || response.code == 404 || response.code == 403) {
            throw ParserIPTVThrowable(false)
        }
        throw Throwable("Retry")
    }.retry { time, throwable ->
        var canRetry = true
        if (throwable is ParserIPTVThrowable) {
            canRetry = throwable.canRetry
        }
        return@retry time < 3 && canRetry
    }.flatMap {
        parseFromStream(extension, it)
    }.doOnSuccess {
        storage.saveLastRefreshExtensions(extension)
    }.doOnComplete {
        storage.saveLastRefreshExtensions(extension)
    }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())


    private fun getKeyValueByRegex(regex: Pattern, finder: String): Pair<String, String> {
        val key = getByRegex(regex, finder)
        val startIndex = finder.indexOf("=")
        val value = finder.substring(startIndex + 1, finder.length)
            .trim()
            .removePrefix("\"")
            .removeSuffix("\r")
            .removeSuffix("\"")
        val realHttpKey = realKeys[key] ?: key
        return Pair(realHttpKey, value)
    }

    private fun parseFromStream(
        config: ExtensionsConfig,
        stream: InputStream
    ): Maybe<MutableList<ExtensionsChannel>> = Observable.create<MutableList<ExtensionsChannel>> { emitter ->
        val reader = stream.bufferedReader()
        var line = reader.readLine()?.trimStart()
        var extensionsChannel: ExtensionsChannel?
        var listChannel = mutableListOf<ExtensionsChannel>()
        var channelId = ""
        var channelLogo = ""
        var channelGroup = ""
        var channelName = ""
        var tvCatchupSource = ""
        var userAgent = ""
        var referer = ""
        var channelLink = ""
        val props = mutableMapOf<String, String>()
        val sourceFrom = config.sourceName
        while (line != null) {
            if (line.trim().isBlank()) {
                line = reader.readLine()?.trimStart()
                continue
            }
            if (line.startsWith(TAG_EXT_INFO) || line.startsWith("EXTINF:")) {
                extensionsChannel = ExtensionsChannel(
                    tvGroup = channelGroup,
                    logoChannel = channelLogo,
                    tvChannelName = channelName.trim(),
                    tvStreamLink = channelLink,
                    sourceFrom = sourceFrom,
                    channelId = channelId,
                    channelPreviewProviderId = -1,
                    isHls = false,
                    extensionSourceId = config.sourceUrl,
                    props = props,
                    userAgent = userAgent,
                    catchupSource = tvCatchupSource,
                    referer = referer
                )

                if (extensionsChannel.isValidChannel) {
                    synchronized(listChannel) {
                        listChannel.add(extensionsChannel)
                        if (listChannel.size > 50) {
                            Logger.d(this@ParserExtensionsSource, "execute", "Insert to db: ${listChannel.size}")
                            if (!emitter.isDisposed) {
                                emitter.onNext(listChannel)
                            }
                            listChannel = mutableListOf()
                        }
                    }
                }
                channelId = ""
                channelLogo = ""
                channelGroup = ""
                channelName = ""
                tvCatchupSource = ""
                userAgent = ""
                referer = ""
                channelLink = ""
                props.clear()
            }

            if (line.contains(URL_TVG_PREFIX)) {
                programScheduleParser.parseForConfig(config, getByRegex(REGEX_PROGRAM_SCHEDULE_URL, line))
            }

            if (line.contains(CATCHUP_SOURCE_PREFIX)) {
                tvCatchupSource = getByRegex(CHANNEL_CATCH_UP_SOURCE_REGEX, line)
            }

            if (line.contains(TAG_USER_AGENT)) {
                userAgent = getByRegex(REGEX_USER_AGENT, line)
            }

            if (line.contains(TAG_REFERER)) {
                referer = getByRegex(REFERER_REGEX, line)
            }

            if (line.removePrefix("#").startsWith("http")) {
                channelLink = line.trim().removePrefix("#").trim()
                    .replace("\t", "")
                    .replace("\b", "")
                    .replace("\r", "")
                    .replace(" ", "")
                    .replace("  ", "")
                    .trim()
                while (channelLink.contains(TAG_REFERER)) {
                    val refererInChannelLink = getByRegex(REFERER_REGEX, line)
                    channelLink = channelLink.replace("$TAG_REFERER=$refererInChannelLink", "")
                        .trim()
                }
                channelLink = channelLink.trim()
                    .removeSuffix("#")
                    .trim()
                if (DEBUG) {
                    Logger.d(this@ParserExtensionsSource, "ChannelLink", channelLink)
                }
            }

            when {
                line.contains(LOGO_PREFIX) || line.contains(ID_PREFIX) || line.contains(TITLE_PREFIX) -> {
                    if (line.contains(ID_PREFIX)) {
                        channelId = getByRegex(CHANNEL_ID_REGEX, line)
                    }

                    if (line.contains(LOGO_PREFIX)) {
                        channelLogo = getByRegex(CHANNEL_LOGO_REGEX, line)
                    }

                    if (line.contains(TITLE_PREFIX)) {
                        channelGroup = getByRegex(CHANNEL_GROUP_TITLE_REGEX, line)
                    }

                    if (line.contains(",")) {
                        channelName = (line.split(",").lastOrNull()
                            ?: "").trim()
                            .removeSuffix(" ")
                            .let {
                                var str = it
                                if (str.contains("Tham gia group")) {
                                    val index = str.indexOf("Tham gia group")
                                    if (index > 0) {
                                        str = str.substring(0, index)
                                    }
                                }
                                if (it.contains("Mời bạn tham gia nhóm Zalo")) {
                                    val index = str.indexOf("Mời bạn tham gia nhóm Zalo")
                                    if (index > 0) {
                                        str = str.substring(0, index)
                                    }
                                }

                                str.trim()
                                    .removeSuffix("-")
                            }
                    }

                }

                line.contains(TAG_EXTVLCOPT) -> {
                    val keyValue = getKeyValueByRegex(REGEX_EXTVLCOPT_PROP_KEY, line)
                    props[keyValue.first] = keyValue.second
                }

                line.contains(TAG_KODIPROP) -> {
                    val keyValue = getKeyValueByRegex(REGEX_KODI_PROP_KEY, line)
                    props[keyValue.first] = keyValue.second
                }
            }

            line = reader.readLine()?.trimStart()
        }
        if (listChannel.isNotEmpty() && !emitter.isDisposed) {
            emitter.onNext(listChannel)
        }
        emitter.onComplete()
    }.observeOn(Schedulers.io()).flatMap { list ->
        Logger.d(this@ParserExtensionsSource, "InsertDB", "$list")
        val listCategory = list.groupBy {
            it.tvGroup
        }.keys.map {
            ExtensionChannelCategory(config.sourceUrl, it)
        }
        val insertCategorySource = roomDataBase.extensionsChannelCategoryDao()
            .insert(listCategory)
        extensionsChannelDao.insert(list)
            .andThen(insertCategorySource)
            .andThen(Observable.just(list))
            .doOnComplete {
                Logger.d(this@ParserExtensionsSource, "InsertDB", "OnComplete insert")
            }.doOnError {
                Logger.e(this@ParserExtensionsSource, "InsertDBFail", exception = it)
            }
    }.reduce { t1, t2 ->
        t1.toMutableList().let {
            it.addAll(t2)
            it
        }
    }

    private fun getByRegex(pattern: Pattern, finder: String): String {
        val matcher = pattern.matcher(finder)
        while (matcher.find()) {
            var str = matcher.group(0)
            var findNextIndexGroup = 1
            val groupCount = matcher.groupCount()
            while (str.isNullOrEmpty() && findNextIndexGroup <= groupCount) {
                str = matcher.group(findNextIndexGroup)
                findNextIndexGroup++
            }
            return str ?: ""
        }
        return ""
    }

    fun insertAll(): Completable {
        return Observable.create<List<ExtensionsConfig>> { emitter ->
            remoteConfig.fetch()
                .addOnSuccessListener { success ->
                        Logger.d(
                            this@ParserExtensionsSource,
                            message = "Default data version from remote: ${remoteConfig.getLong("default_iptv_version")}"
                        )
                        Logger.d(
                            this@ParserExtensionsSource,
                            message = "Cache version: ${storage.get("default_iptv_version", Long::class.java)}"
                        )
                        Logger.d(
                            this@ParserExtensionsSource,
                            message = remoteConfig.getString("default_iptv_channel")
                        )
                        val defaultIptvVersion = remoteConfig.getLong("default_iptv_version")
                        val localIptvVersion = storage.get("default_iptv_version", Long::class.java)
                        if (defaultIptvVersion > localIptvVersion) {
                            storage.save("beta_insert_default_source", false)
                        }
                        if (!storage.get("beta_insert_default_source", Boolean::class.java)) {
                            val jsonArray: JSONArray = try {
                                JSONArray(remoteConfig.getString("default_iptv_channel"))
                            } catch (e: Exception) {
                                emitter.onError(e)
                                return@addOnSuccessListener
                            }

                            val defaultList = mutableListOf<ExtensionsConfig>()

                            if (jsonArray.length() > 0) {
                                for (i in 0 until jsonArray.length()) {
                                    val jsonObject = jsonArray.optJSONObject(i)
                                    val sourceName = jsonObject.optString("sourceName")
                                    val sourceUrl = jsonObject.optString("sourceUrl")
                                    val type = try {
                                        ExtensionsConfig.Type.valueOf(jsonObject.optString("type"))
                                    } catch (e: Exception) {
                                        ExtensionsConfig.Type.TV_CHANNEL
                                    }
                                    defaultList.add(
                                        ExtensionsConfig(
                                            sourceUrl = sourceUrl,
                                            sourceName = sourceName,
                                            type = type
                                        )
                                    )
                                }
                                if (!emitter.isDisposed) {
                                    emitter.onNext(defaultList)
                                    storage.save("default_iptv_version", defaultIptvVersion)
                                    emitter.onComplete()
                                }
                            }
                        }
                }
                .addOnFailureListener {
                    if (!emitter.isDisposed) {
                        emitter.onError(it)
                    }
                }
        }
            .retry { t1, _ ->
                return@retry t1 < 3
            }
            .concatMapCompletable {
                roomDataBase.extensionsConfig()
                    .insertAll(*it.toTypedArray())
                    .subscribeOn(Schedulers.io())
                    .doOnComplete {
                        storage.save("beta_insert_default_source", true)
                    }
            }
    }


    private fun getByRegex(regex: String, finder: String): String {
        val pt = Pattern.compile(regex)
        return getByRegex(pt, finder)
    }

    fun insertIptvSource(extensionsConfig: ExtensionsConfig) = roomDataBase.extensionsConfig()
        .insert(extensionsConfig)
        .subscribeOn(Schedulers.io())

    fun isSourceExist(configId: String): Single<Boolean> {
        return roomDataBase.extensionsConfig()
            .checkExtensionById(configId)
            .subscribeOn(Schedulers.io())
            .map { count ->
                count > 0
            }
    }

    fun updateIPTVSource(extensionsConfig: ExtensionsConfig): Completable {
        return roomDataBase.extensionsConfig()
            .update(extensionsConfig)
            .subscribeOn(Schedulers.io())
    }

    class ParserIPTVThrowable(
        val canRetry: Boolean,
        override val message: String? = null,
    ) : Throwable(message)

    companion object {
        private const val DEBUG = false
        private const val EXTRA_INTERVAL_REFRESH_DATA_KEY = "extra:interval_refresh_data"
        private const val INTERVAL_REFRESH_DATA_TV_CHANNEL: Long = 60 * 60 * 1000
        private const val INTERVAL_REFRESH_DATA_MOVIE: Long = 24 * 60 * 60 * 1000
        private const val INTERVAL_REFRESH_DATA_FOOTBALL: Long = 15 * 60 * 1000
        private const val OFFSET_TIME = 2 * 60 * 60 * 1000
        private const val EXTRA_EXTENSIONS_KEY = "extra:extensions_key"
        private const val TAG_START = "#EXTM3U"
        private const val TAG_EXT_INFO = "#EXTINF:"
        private const val TAG_REFERER = "|Referer"
        private val REGEX_MEDIA_DURATION = Pattern.compile("#EXTINF:( )?-?\\d+")
        private val REGEX_MEDIA_DURATION_2 = Pattern.compile("EXTINF:( )?-?\\d+")
        private const val URL_TVG_PREFIX = "url-tvg"
        private const val CACHE_PREFIX = "cache"
        private const val RATIO_PREFIX = "aspect-ratio"
        private const val DEINTERLACE_PREFIX = "deinterlace"
        private const val TVG_SHIFT_PREFIX = "tvg-shift"
        private const val M3U_AUTO_LOAD_PREFIX = "m3uautoload"
        private const val CATCHUP_SOURCE_PREFIX = "catchup-source"
        private const val TAG_USER_AGENT = "user-agent"
        private const val TAG_KODIPROP = "KODIPROP"
        private const val TAG_EXTVLCOPT = "EXTVLCOPT"
        private val REGEX_USER_AGENT = Pattern.compile("(?<=user-agent=\").*?(?=\")")

        private const val ID_PREFIX = "tvg-id"
        private const val LOGO_PREFIX = "tvg-logo"
        private const val TITLE_PREFIX = "group-title"
        private const val TYPE_PREFIX = "type"

        private val URL_TVG_REGEX = Pattern.compile("(?<=url-tvg=\").*?(?=\")")
        private val CACHE_REGEX = Pattern.compile("(?<=cache=).*?(?= )")
        private val DEINTERLACE_REGEX = Pattern.compile("(?<=deinterlace=).*?(?= )")
        private val RATIO_REGEX = Pattern.compile("(?<=aspect-ratio=).*?(?= )")
        private val TVG_SHIFT_REGEX = Pattern.compile("(?<=tvg-shift=).*?(?= )")
        private val M3U_AUTO_REGEX = Pattern.compile("(?<=m3uautoload=).*?(?= )")
        private val CHANNEL_ID_REGEX = Pattern.compile("(?<=tvg-id=\").*?(?=\")")
        private val CHANNEL_LOGO_REGEX = Pattern.compile("(?<=tvg-logo=\").*?(?=\")")
        private val CHANNEL_GROUP_TITLE_REGEX = Pattern.compile("(?<=group-title=\").*?(?=\")")
        private val CHANNEL_CATCH_UP_SOURCE_REGEX = Pattern.compile("(?<=catchup-source=\").*?(?=\")")
        private val REFERER_REGEX = Pattern.compile("(?<=\\|Referer=).*")
        private val CHANNEL_TYPE_REGEX = Pattern.compile("(?<=type=\").*?(?=\")")
        private val CHANNEL_TITLE_REGEX = Pattern.compile("(?<=\").*?(?=\")")
        private val REGEX_KODI_PROP_KEY = Pattern.compile("(?<=KODIPROP:).*?(?==)")
        private val REGEX_EXTVLCOPT_PROP_KEY = Pattern.compile("(?<=EXTVLCOPT:).*?(?==)")
        private val REGEX_PROGRAM_SCHEDULE_URL = Pattern.compile("(?<=url-tvg=\").*?(?=\")")
        private val realKeys = mapOf(
            "http-referrer" to "referer",
            "http-user-agent" to "user-agent"
        )

        val filmData = mapOf(
            "Phim lẻ TVHay" to "http://hqth.me/tvhayphimle",
            "Phim lẻ FPTPlay" to "http://hqth.me/jsfptphimle",
            "Phim bộ" to "http://hqth.me/phimbo",
            "Phim miễn phí" to "https://hqth.me/phimfree",
            "Film" to "https://gg.gg/films24",
        )

        val mapBongDa: Map<String, String> = mapOf(
            "Bóng đá" to "http://gg.gg/SN-90phut",
        )

        val tvChannel: Map<String, String> by lazy {
            mapOf(
                "K+" to "https://s.id/nhamng",
                "VThanhTV" to "http://vthanhtivi.pw",
            )
        }

        private fun Map<String, String>.mapToListExConfig(type: ExtensionsConfig.Type) = map {
            ExtensionsConfig(
                sourceName = it.key,
                sourceUrl = it.value,
                type = type
            )
        }
    }

}