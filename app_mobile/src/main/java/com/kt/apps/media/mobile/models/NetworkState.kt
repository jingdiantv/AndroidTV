package com.kt.apps.media.mobile.models

enum class NetworkState {
    Connected, Unavailable
}

class NoNetworkException: Throwable("No network")
