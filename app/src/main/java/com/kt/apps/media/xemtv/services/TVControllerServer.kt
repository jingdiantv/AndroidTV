package com.kt.apps.media.xemtv.services

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import javax.inject.Inject

class TVControllerServer @Inject constructor() {


    fun receiveData() {

    }

    fun getIPAddress(useIPv4: Boolean): String {
        try {
            val networkInterfaces: List<NetworkInterface> = NetworkInterface.getNetworkInterfaces().toList()
            for (networkInterface in networkInterfaces) {
                val inetAddresses: List<InetAddress> = networkInterface.inetAddresses.toList()
                for (address in inetAddresses) {
                    if (!address.isLoopbackAddress) {
                        val hostAddress: String = address.hostAddress ?: ""
                        val isIPv4 = hostAddress.indexOf(':') < 0
                        if (useIPv4) {
                            if (isIPv4) return hostAddress
                        } else {
                            if (!isIPv4) {
                                val delim = hostAddress.indexOf('%')
                                return if (delim < 0) hostAddress.toUpperCase(Locale.ROOT)
                                else hostAddress.substring(0, delim).toUpperCase(Locale.ROOT)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "0.0.0.0"
    }
}