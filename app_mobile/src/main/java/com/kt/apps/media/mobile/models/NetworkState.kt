package com.kt.apps.media.mobile.models

import com.google.android.exoplayer2.PlaybackException

enum class NetworkState {
    Connected, Unavailable
}

class NoNetworkException: Throwable("No network")
class PlaybackFailException(val error: PlaybackException): Throwable()