package com.kt.apps.core.logging

import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.upstream.HttpDataSource

interface IActionLogger {
    fun log(event: String, extras: Bundle)
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