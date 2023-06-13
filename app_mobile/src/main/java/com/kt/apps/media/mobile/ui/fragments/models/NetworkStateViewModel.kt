package com.kt.apps.media.mobile.ui.fragments.models

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.media.mobile.App
import com.kt.apps.media.mobile.models.NetworkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class NetworkStateViewModel @Inject constructor(private val app: App) : BaseViewModel() {
    private val connectivityManager by lazy {
        app
            .applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback by lazy {
        object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                _networkStatus.value = NetworkState.Connected
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                _networkStatus.value = NetworkState.Unavailable
            }
        }
    }

    private val _networkStatus = MutableStateFlow<NetworkState>(NetworkState.Connected)
    val networkStatus: StateFlow<NetworkState>
        get() = _networkStatus

    init {
        connectivityManager
            .registerNetworkCallback(
                NetworkRequest.Builder().apply {
                    addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                }.build(),
                networkCallback
            )
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}