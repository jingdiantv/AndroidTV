package com.kt.apps.core.logging

import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import com.kt.apps.core.usecase.search.SearchForText

interface IActionLogger {
    fun log(event: String, extras: Bundle)
}

fun IActionLogger.logAddIPTVSource(
    configUrl: String,
    configName: String,
    vararg extras: Pair<String, Any?>
) {
    this.log(
        "AddIPTV",
        bundleOf(
            "iptv_url" to configUrl,
            "iptv_name" to configName,
            *extras
        )
    )
}

fun IActionLogger.logStreamingTV(
    channelName: String,
    vararg extras: Pair<String, Any?>
) {
    this.log(
        "Streaming", bundleOf(
            "channel" to channelName,
            *extras
        )
    )
}

fun IActionLogger.logPlayByDeeplinkTV(
    uri: Uri,
    channelName: String,
    vararg extras: Pair<String, Any?>
) {
    this.log(
        "PlayByDeepLink", bundleOf(
            "uri" to "$uri",
            "channel" to channelName,
            *extras
        )
    )
}

fun IActionLogger.logPlaybackError(
    event: String,
    error: PlaybackException,
    channel: String,
    vararg extras: Pair<String, Any?>
) {
    this.log(
        event,
        bundleOf(
            "playbackException" to error.message,
            "errorCode" to error.errorCode,
            "httpErrorCode" to if (error.cause is HttpDataSource.InvalidResponseCodeException) {
                (error.cause as HttpDataSource.InvalidResponseCodeException).responseCode
            } else {
                0
            },
            "channel" to channel,
            *extras
        )
    )
}

fun IActionLogger.logPlaybackShowError(
    error: PlaybackException,
    channel: String,
    vararg extras: Pair<String, Any?>

) {
    this.logPlaybackError(
        "PlaybackShowError",
        error,
        channel,
        *extras
    )
}

fun IActionLogger.logPlaybackRetryPlayVideo(
    error: PlaybackException,
    channelName: String,
    vararg extras: Pair<String, Any?>
) {
    this.logPlaybackError(
        "PlaybackRetryPlayVideo",
        error,
        channelName,
        *extras
    )
}

fun IActionLogger.logPlaybackRetryGetStreamLink(
    error: PlaybackException,
    channelName: String,
    vararg extras: Pair<String, Any?>
) {
    this.logPlaybackError(
        "PlaybackRetryGetStreamLink",
        error,
        channelName,
        *extras
    )
}

fun IActionLogger.logSearchForText(
    query: String,
    queryResultCount: Int,
    vararg extras: Pair<String, Any?>
) {
    this.log(
        "Search",
        extras = bundleOf(
            "SearchQuery" to query,
            "SearchResultCount" to queryResultCount,
            *extras
        )
    )
}

fun IActionLogger.logSearchForTextAndPerformClick(
    query: String,
    searchResult: SearchForText.SearchResult,
    vararg extras: Pair<String, Any?>
) {
    this.log(
        "SearchAndPerformClick",
        extras = bundleOf(
            "SearchQuery" to query,
            "Type" to when (searchResult) {
                is SearchForText.SearchResult.TV -> "TV"
                is SearchForText.SearchResult.History -> "History"
                is SearchForText.SearchResult.ExtensionsChannelWithCategory -> "IPTV"
            },
            "SearchResultTitle" to when (searchResult) {
                is SearchForText.SearchResult.TV -> searchResult.data.tvChannelName
                is SearchForText.SearchResult.History -> searchResult.data.displayName
                is SearchForText.SearchResult.ExtensionsChannelWithCategory -> searchResult.data.tvChannelName
            },
            "SearchResultData" to "$searchResult",
            *extras
        )
    )
}

fun IActionLogger.logShowHistoryForItemDialog(
    item: HistoryMediaItemDTO
) {

}

fun IActionLogger.logClickYesHistoryDialog(
    item: HistoryMediaItemDTO
) {

}

fun IActionLogger.logClickNoHistoryDialog(
    item: HistoryMediaItemDTO
) {

}

