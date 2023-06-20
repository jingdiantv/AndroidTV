package com.kt.apps.core.base.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import java.lang.Exception
import java.lang.ref.WeakReference


class NetworkChangeReceiver : BroadcastReceiver() {
    interface OnNetworkChangeListener {
        fun onChange(isOnline: Boolean)
    }

    private val _listener by lazy {
        hashMapOf<String, OnNetworkChangeListener>()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return
        _listener[context::class.java.name]?.onChange(context.isNetworkAvailable())
    }

    companion object {
        private var instance: WeakReference<NetworkChangeReceiver>? = null

        fun getInstance(): NetworkChangeReceiver {
            WifiManager.SUPPLICANT_STATE_CHANGED_ACTION
            return instance?.get() ?: NetworkChangeReceiver().apply {
                instance = WeakReference(this)
            }
        }

        fun Context.registerNetworkChangeReceiver(
            receiver: NetworkChangeReceiver,
            listener: OnNetworkChangeListener
        ) {
            receiver._listener[this::class.java.name] = listener
            registerReceiver(
                receiver,
                IntentFilter().apply {
                    addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                }
            )
        }

        fun Context.unregisterNetworkChangeReceiver(receiver: NetworkChangeReceiver) {
            try {
                unregisterReceiver(receiver)
                receiver._listener.remove(this::class.java.name)
            } catch (_: Exception) {
            }
        }

        fun Context.isNetworkAvailable(): Boolean {
            val netInfo = (this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
            return netInfo != null && netInfo.isConnected
        }
    }
}