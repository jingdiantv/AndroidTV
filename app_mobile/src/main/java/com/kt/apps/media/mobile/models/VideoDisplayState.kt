package com.kt.apps.media.mobile.models

import com.google.android.exoplayer2.video.VideoSize

sealed class VideoDisplayState {
    object IDLE: VideoDisplayState()
    object LOADING: VideoDisplayState()
    data class SUCCESS(val videoSize: VideoSize): VideoDisplayState()
    object FULLSCREEN:  VideoDisplayState()
}