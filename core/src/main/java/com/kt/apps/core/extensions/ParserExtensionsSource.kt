package com.kt.apps.core.extensions

import android.net.Uri
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.local.RoomDataBase
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
    private val db: RoomDataBase
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
            return parseFromText(bodyStr, extension)
        }
        return emptyList()
    }

    private fun parseFromText(text: String, extension: ExtensionsConfig): List<ExtensionsChannel> {
        return text.split("#EXTINF:-1")
            .map {
                Logger.e(this@ParserExtensionsSource, message = "================")
                Logger.e(this@ParserExtensionsSource, message = it)
                Logger.e(this@ParserExtensionsSource, message = "================")
                it.trim().removePrefix(",")
            }
            .map {
                var itemStr = it
                var channelId = ""
                var channelLogo = ""
                var channelGroup = ""

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
                    itemStr = itemStr.replace("$TITLE_PREFIX=\"$channelLogo\"", "")
                }

                val lastHttpIndex = itemStr.lastIndexOf("http")
                val channelLink = itemStr.substring(lastHttpIndex, itemStr.length)
                try {
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

                    val channel = ExtensionsChannel(
                        tvGroup = channelGroup,
                        logoChannel = Uri.parse(channelLogo),
                        channelId = channelId,
                        tvChannelName = channelName.trim().removeSuffix(" "),
                        sourceFrom = extension.sourceName,
                        tvStreamLink = channelLink.trim().removeSuffix(" "),
                        isHls = channelLink.contains("m3u8")
                    )

                    Logger.d(
                        this, message = "${Gson().toJson(channel)}"
                    )
                    channel
                } catch (e: Exception) {
                    Logger.e(this, exception = e)
                    throw e
                }
            }
    }

    data class ExtensionsChannel(
        val tvGroup: String,
        val logoChannel: Uri,
        val tvChannelName: String,
        val tvStreamLink: String,
        val sourceFrom: String,
        @PrimaryKey
        val channelId: String,
        val channelPreviewProviderId: Long = -1,
        val isHls: Boolean
    ) {
    }

    private fun getByRegex(pattern: Pattern, finder: String): String {
        val matcher = pattern.matcher(finder)
        while (matcher.find()) {
            return matcher.group(0)
        }
        return ""
    }


    private fun getByRegex(regex: String, finder: String): String {
        val pt = Pattern.compile(regex)
        return getByRegex(pt, finder)
    }

    companion object {
        private const val EXTRA_EXTENSIONS_KEY = "extra:extensions_key"

        private const val TAG_START = "#EXTM3U"
        private const val URL_TVG_PREFIX = "url-tvg"
        private const val CACHE_PREFIX = "cache"
        private const val RATIO_PREFIX = "aspect-ratio"
        private const val DEINTERLACE_PREFIX = "deinterlace"
        private const val TVG_SHIFT_PREFIX = "tvg-shift"
        private const val M3U_AUTO_LOAD_PREFIX = "m3uautoload"

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
        private val CHANNEL_TYPE_REGEX = Pattern.compile("(?<=type=\").*?(?=\")")
        private val CHANNEL_TITLE_REGEX = Pattern.compile("(?<=\").*?(?=\")")
    }

}