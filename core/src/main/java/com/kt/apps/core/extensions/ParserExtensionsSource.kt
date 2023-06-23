package com.kt.apps.core.extensions

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.di.NetworkModule
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.getLastRefreshExtensions
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.ExtensionChannelCategory
import com.kt.apps.core.storage.removeLastRefreshExtensions
import com.kt.apps.core.storage.saveLastRefreshExtensions
import com.kt.apps.core.utils.trustEveryone
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.DisposableContainer
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.lang.NullPointerException
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

    private val pendingSource: MutableMap<String, Observable<List<ExtensionsChannel>>> by lazy {
        mutableMapOf()
    }


    fun parseFromRemoteMaybe(extension: ExtensionsConfig): Maybe<List<ExtensionsChannel>> {
        return Maybe.fromCallable {
            return@fromCallable parseFromRemote(extension)
        }
    }

    fun parseFromRemoteRx(extension: ExtensionsConfig): Observable<List<ExtensionsChannel>> {
        if (pendingSource.contains(extension.sourceUrl)) {
            return pendingSource[extension.sourceUrl]!!
        }
        val onlineSource = Observable.create<List<ExtensionsChannel>> {
            try {
                while (pendingSource.size > 5) {
                    Thread.sleep(100)
                }
                val totalList = parseFromRemote(extension)
                if (totalList.isEmpty()) {
                    storage.removeLastRefreshExtensions(extension)
                    disposable.add(extensionsConfigDao.delete(extension).subscribe({}, {}))
                    if (it.isDisposed) {
                        return@create
                    }
                    it.onError(Throwable("Định dạng nguồn kênh không hợp lệ"))
                } else {
                    if (it.isDisposed) {
                        return@create
                    }
                    it.onNext(totalList)
                    it.onComplete()
                }
            } catch (e: Exception) {
                if (it.isDisposed) {
                    return@create
                }
                it.onError(e)
            }
        }
            .retry { time, throwable ->
                var canRetry = true
                if (throwable is ParserIPTVThrowable) {
                    canRetry = throwable.canRetry
                }
                return@retry time < 3 && canRetry
            }
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .doOnNext {
                if (DEBUG) {
                    Logger.d(this@ParserExtensionsSource, message = Gson().toJson(it))
                }
                Logger.d(this@ParserExtensionsSource, "execute", "insert db ${it.size}")
                disposable.add(
                    extensionsChannelDao.insert(it)
                        .doOnComplete {
                            Logger.d(this@ParserExtensionsSource, "execute", "insert complete")
                            storage.saveLastRefreshExtensions(extension)
                        }
                        .subscribe({}, {}),

                    )
                disposable.add(
                    roomDataBase.extensionsChannelCategoryDao()
                        .insert(
                            it.groupBy {
                                it.tvGroup
                            }.keys
                                .map {
                                    ExtensionChannelCategory(extension.sourceUrl, it)
                                }
                        )
                        .subscribe()
                )
            }
            .doOnComplete {
                pendingSource.remove(extension.sourceUrl)
            }

        val offlineSource = extensionsChannelDao.getAllBySourceId(extension.sourceUrl)
            .toObservable()
            .flatMap {
                if (it.isEmpty()) {
                    onlineSource
                } else {
                    Observable.just(it)
                }
            }
            .doOnEach {
                programScheduleParser.parseForConfig(extension)
            }

        Logger.d(this@ParserExtensionsSource, "execute", "Old time: ${storage.getLastRefreshExtensions(extension)}")

        if (System.currentTimeMillis() - storage.getLastRefreshExtensions(extension) < getIntervalRefreshData(extension.type)) {
            Logger.d(this@ParserExtensionsSource, "execute", "OfflineSource")
            val source = offlineSource
                .onErrorResumeNext {
                    onlineSource
                }
                .doOnComplete {
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

    fun parseFromRemote(extension: ExtensionsConfig): List<ExtensionsChannel> {
        if (!(extension.sourceUrl.startsWith("http://") || extension.sourceUrl.startsWith("https://"))) {
            return emptyList()
        }
        trustEveryone()
        val response = client
            .newBuilder()
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
            .newCall(
                Request.Builder()
                    .url(extension.sourceUrl)
                    .build()

            ).execute()

        if (response.code in 200..299) {
            val bodyStr = response.body.string()
            Logger.d(
                this@ParserExtensionsSource,
                message = "${extension.sourceUrl} - Content Length: $${bodyStr.length}"
            )
            response.body.close()
            val index = bodyStr.indexOf(TAG_EXT_INFO)
            programScheduleParser.parseForConfig(extension, getByRegex(REGEX_PROGRAM_SCHEDULE_URL, bodyStr))
            return parseFromText(bodyStr.substring(maxOf(0, index), bodyStr.length), extension)
        }
        if (response.code >= 500 || response.code == 404 || response.code == 403) {
            throw ParserIPTVThrowable(false)
        }
        return emptyList()
    }

    private fun parseFromText(text: String, extension: ExtensionsConfig): List<ExtensionsChannel> {
        return text.split(REGEX_MEDIA_DURATION)
            .map {
                it.trim().removePrefix(",")
            }.mapNotNull { itemStr ->
                extractChannel(itemStr, extension)
            }
            .distinctBy {
                "${it.tvGroup}${it.tvStreamLink}${it.channelId}"
            }
    }

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

    private fun extractChannel(
        itemStr: String,
        extension: ExtensionsConfig
    ): ExtensionsChannel? {
        var channelId = ""
        var channelLogo = ""
        var channelGroup = ""
        var channelName = ""
        var tvCatchupSource = ""
        var userAgent = ""
        var referer = ""
        var channelLink = ""
        val props = mutableMapOf<String, String>()

        val listChannelInfoStr = itemStr.split("\n", "#")
            .filter {
                it.isNotBlank()
            }

        if (listChannelInfoStr.isEmpty()) {
            return null
        }

        val lastLine = listChannelInfoStr.last()
        channelLink = lastLine.trim().removePrefix("#").trim()
            .replace("\t", "")
            .replace("\b", "")
            .replace("\r", "")
            .replace(" ", "")
            .replace("  ", "")
            .trim()
        while (channelLink.contains(TAG_REFERER)) {
            val refererInChannelLink = getByRegex(REFERER_REGEX, lastLine)
            channelLink = channelLink.replace("$TAG_REFERER=$refererInChannelLink", "")
                .trim()
        }
        channelLink = channelLink.trim()
            .removeSuffix("#")
            .trim()

        if (DEBUG) {
            Logger.d(this@ParserExtensionsSource, "ChannelLink", channelLink)
        }
        listChannelInfoStr.forEach { line ->
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

            if (line.contains(CATCHUP_SOURCE_PREFIX)) {
                tvCatchupSource = getByRegex(CHANNEL_CATCH_UP_SOURCE_REGEX, line)
            }

            if (line.contains(TAG_USER_AGENT)) {
                userAgent = getByRegex(REGEX_USER_AGENT, line)
            }

            if (line.contains(TAG_REFERER)) {
                referer = getByRegex(REFERER_REGEX, line)
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
                        channelName = line.split(",").lastOrNull()
                            ?: ""
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
        }
        return try {
            if (channelLink.isBlank() && tvCatchupSource.isBlank()) {
                throw NullPointerException()
            }
            val channel = ExtensionsChannel(
                tvGroup = channelGroup,
                logoChannel = channelLogo.trim(),
                channelId = channelId,
                tvChannelName = channelName.trim()
                    .removeSuffix(" ")
                    .let {
                        var str = it
                        if (str.contains("Tham gia group")) {
                            val index = str.indexOf("Tham gia group")
                            if (index > 0) {
                                str = str.substring(0, index)
                            }
                        }
                        if (str.contains("Donate")
                            || str.lowercase().startsWith("tham gia group")
                            || str.lowercase().startsWith("nhóm zalo")) {
                            throw NullPointerException("Not valid channel name: $channelName")
                        }
                        if (it.contains("Mời bạn tham gia nhóm Zalo")) {
                            val index = str.indexOf("Mời bạn tham gia nhóm Zalo")
                            if (index > 0) {
                                str = str.substring(0, index)
                            }
                        }

                        str.trim()
                            .removeSuffix("-")
                    },
                sourceFrom = extension.sourceName,
                tvStreamLink = channelLink.trim().removeSuffix(" "),
                isHls = channelLink.contains("m3u8"),
                catchupSource = tvCatchupSource.replace("\${start}", "${System.currentTimeMillis()}")
                    .replace("\${offset}", "${System.currentTimeMillis() + OFFSET_TIME}"),
                referer = referer,
                userAgent = userAgent,
                props = props,
                extensionSourceId = extension.sourceUrl
            )
            if (!channel.isValidChannel) {
                throw NullPointerException("Channel not valid: $channel")
            }
            if (DEBUG) {
                Logger.d(this@ParserExtensionsSource, "Channel", message = "$channel")
            }
            channel
        } catch (e: Exception) {
            Logger.e(this, message = "Parser error: $itemStr")
            Logger.e(this, exception = e)
            null
        }
    }

    private fun getByRegex(pattern: Pattern, finder: String): String {
        val matcher = pattern.matcher(finder)
        while (matcher.find()) {
            return matcher.group(0) ?: ""
        }
        return ""
    }

    fun insertAll(): Completable {
        return Observable.create<List<ExtensionsConfig>> { emitter ->
            remoteConfig.fetch()
                .addOnSuccessListener { success ->
                        Logger.d(
                            this@ParserExtensionsSource,
                            message = "${remoteConfig.getLong("default_iptv_version")}"
                        )
                        Logger.d(
                            this@ParserExtensionsSource,
                            message = "${storage.get("default_iptv_version", Long::class.java)}"
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