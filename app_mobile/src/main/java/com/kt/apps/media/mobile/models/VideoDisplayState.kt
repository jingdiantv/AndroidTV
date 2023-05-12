package com.kt.apps.media.mobile.models

import com.google.android.exoplayer2.video.VideoSize

sealed class VideoDisplayAction {
    data class SUCCESS(val videoSize: VideoSize): VideoDisplayAction()
}