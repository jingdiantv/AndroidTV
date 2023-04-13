package com.kt.apps.core.extensions

import com.google.gson.Gson
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.utils.trustEveryone
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

@CoreScope
class ParserExtensionsSource @Inject constructor(
    private val client: OkHttpClient,
    private val storage: IKeyValueStorage,
) {

    fun parseFromRemoteRx(extension: ExtensionsConfig): Observable<List<ExtensionsChannel>> {
        return Observable.create<List<ExtensionsChannel>> {
            try {
                it.onNext(parseFromRemote(extension))
                it.onComplete()
            } catch (e: Exception) {
                it.onError(e)
            }
        }
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .doOnNext {
                Logger.d(this@ParserExtensionsSource, message = Gson().toJson(it))
            }

    }

    fun parseFromRemote(extension: ExtensionsConfig): List<ExtensionsChannel> {
        trustEveryone()
        val response = client
            .newBuilder()
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
            .newCall(
                Request.Builder()
                    .url(extension.sourceUrl)
                    .build()
            ).execute()

        if (response.code in 200..299) {
            val bodyStr = response.body.string()
            if (!bodyStr.trim().startsWith(TAG_START)) {
                throw IllegalStateException("Not support for extension ${extension.sourceUrl} cause: " +
                        "Source must start with $TAG_START")
            }
            val index = bodyStr.indexOf(TAG_EXT_INFO)

            return parseFromText(bodyStr.substring(index, bodyStr.length), extension)
        }
        return emptyList()
    }

    private fun parseFromText(text: String, extension: ExtensionsConfig): List<ExtensionsChannel> {
        return text.split(REGEX_MEDIA_DURATION)
            .map {
                it.trim().removePrefix(",")
            }
            .map {
                Logger.e(this@ParserExtensionsSource, message = "=================")
                Logger.e(this@ParserExtensionsSource, message = it)
                Logger.e(this@ParserExtensionsSource, message = "=================")
                var itemStr = it
                var channelId = ""
                var channelLogo = ""
                var channelGroup = ""
                var tvCatchupSource = ""
                var userAgent = ""
                var referer = ""

                if (itemStr.contains(ID_PREFIX)) {
                    channelId = getByRegex(CHANNEL_ID_REGEX, itemStr)
                    itemStr = itemStr.replace("$ID_PREFIX=\"$channelId\"", "")
                }

                if (itemStr.contains(LOGO_PREFIX)) {
                    channelLogo = getByRegex(CHANNEL_LOGO_REGEX, itemStr)
                    itemStr = itemStr.replace("$LOGO_PREFIX=\"$channelLogo\"", "")
                }

                if (itemStr.contains(TITLE_PREFIX)) {
                    channelGroup = getByRegex(CHANNEL_GROUP_TITLE_REGEX, itemStr)
                    itemStr = itemStr.replace("$TITLE_PREFIX=\"$channelGroup\"", "")
                }

                if (itemStr.contains(CATCHUP_SOURCE_PREFIX)) {
                    tvCatchupSource = getByRegex(CHANNEL_CATCH_UP_SOURCE_REGEX, itemStr)
                }

                if (itemStr.contains(TAG_USER_AGENT)) {
                    userAgent = getByRegex(REGEX_USER_AGENT, itemStr)
                    if (userAgent.isNotEmpty()) {
                        itemStr = itemStr.replace("$TAG_USER_AGENT=\"$userAgent\"", "")
                    }
                }

                if (itemStr.contains(TAG_REFERER)) {
                    Logger.d(this@ParserExtensionsSource, "Referer", itemStr)
                    Logger.d(this@ParserExtensionsSource, "Referer", getByRegex(REFERER_REGEX, itemStr))
                    referer = getByRegex(REFERER_REGEX, itemStr)
                    if (referer.isNotBlank()) {
                        itemStr = itemStr.replace("|Referer=$referer", "")
                            .trim()
                            .removeSuffix("#")
                    }
                    Logger.d(this@ParserExtensionsSource, "Referer", itemStr)
                }

                val lastHttpIndex = itemStr.lastIndexOf("http")
                try {
                    Logger.d(this@ParserExtensionsSource, message = "$lastHttpIndex - ${itemStr.lastIndexOf("\r\n")}")
                    val lastEndLineIndex = itemStr.lastIndexOf("\r\n")
                    val channelLink = if (lastEndLineIndex > lastHttpIndex) {
                        itemStr.substring(lastHttpIndex, lastEndLineIndex)
                    } else {
                        itemStr.substring(lastHttpIndex, itemStr.length)
                    }
                    val splitIndex = itemStr.indexOf(",")
                    val lastExtTag = itemStr.lastIndexOf("#EXT")
                    val startLinkIndex = itemStr.indexOf(channelLink)
                    val channelName = if (lastExtTag != -1 && lastExtTag > splitIndex) {
                        itemStr.substring(splitIndex + 1, lastExtTag)
                    } else if (startLinkIndex > splitIndex) {
                        itemStr.substring(splitIndex + 1, startLinkIndex)
                    } else {
                        ""
                    }
                        .trim()
                        .removeSuffix("#")
                        .removeSuffix("\r\n")
                        .removePrefix(",")

                    Logger.e(this@ParserExtensionsSource, message = "Channel Link: $channelLink")
                    if (channelLink.isBlank() && tvCatchupSource.isBlank()) {
                        throw java.lang.NullPointerException()
                    }

                    val channel = ExtensionsChannel(
                        tvGroup = channelGroup,
                        logoChannel = channelLogo,
                        channelId = channelId,
                        tvChannelName = channelName.trim().removeSuffix(" "),
                        sourceFrom = extension.sourceName,
                        tvStreamLink = channelLink.trim().removeSuffix(" "),
                        isHls = channelLink.contains("m3u8"),
                        catchupSource = tvCatchupSource.replace("\${start}", "${System.currentTimeMillis()}")
                            .replace("\${offset}", "${System.currentTimeMillis() + OFFSET_TIME}"),
                        referer = referer,
                        userAgent = userAgent
                    )
                    channel
                } catch (e: Exception) {
                    Logger.e(this, message = itemStr)
                    Logger.e(this, exception = e)
                    null
                }

            }
            .filterNotNull()
    }

    private fun getByRegex(pattern: Pattern, finder: String): String {
        val matcher = pattern.matcher(finder)
        while (matcher.find()) {
            return matcher.group(0) ?: ""
        }
        return ""
    }


    private fun getByRegex(regex: String, finder: String): String {
        val pt = Pattern.compile(regex)
        return getByRegex(pt, finder)
    }

    companion object {
        private const val OFFSET_TIME = 4 * 60 * 60 * 1000
        private const val EXTRA_EXTENSIONS_KEY = "extra:extensions_key"
        private const val TAG_START = "#EXTM3U"
        private const val TAG_EXT_INFO = "#EXTINF:"
        private const val TAG_REFERER = "|Referer"
        private val REGEX_MEDIA_DURATION = Pattern.compile("#EXTINF:( )?-?\\d+")
        private const val URL_TVG_PREFIX = "url-tvg"
        private const val CACHE_PREFIX = "cache"
        private const val RATIO_PREFIX = "aspect-ratio"
        private const val DEINTERLACE_PREFIX = "deinterlace"
        private const val TVG_SHIFT_PREFIX = "tvg-shift"
        private const val M3U_AUTO_LOAD_PREFIX = "m3uautoload"
        private const val CATCHUP_SOURCE_PREFIX = "catchup-source"
        private const val TAG_USER_AGENT = "user-agent"
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
    }

}