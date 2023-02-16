package com.kt.apps.core.tv

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.kt.apps.core.tv.model.TVChannel

object FirebaseLogUtils {
    private val log by lazy {
        Firebase.analytics.apply {
            setDefaultEventParameters(Bundle().apply {
            })
        }
    }

    fun logOpenAppEvent(fromDeepLink: String? = null) {
        val params = fromDeepLink?.let {
            bundleOf("deepLink" to it)
        } ?: bundleOf()
        log.logEvent("openApp", params)
    }

    fun logViewVideo(channel: String) {
        log.logEvent(
            "viewVideo", bundleOf(
                "channel" to channel
            )
        )
    }

    fun logViewVideoError(channel: TVChannel, reason: String) {
        log.logEvent(
            "errorViewVideo", bundleOf(
                "channel" to channel.tvChannelName,
                "sourceFrom" to channel.sourceFrom,
                "reason" to reason
            )
        )
    }

    fun logGetLinkVideoM3u8(channel: TVChannel) {
        log.logEvent(
            "GetLinkM3u8Video",
            bundleOf(
                "channel" to channel.tvChannelName,
                "sourceFrom" to channel.sourceFrom
            ),
        )
    }

    fun logGetLinkVideoM3u8Error(
        tvDetail: TVChannel,
        reason: String
    ) {
        log.logEvent(
            "ErrorGetLinkM3u8Video", bundleOf(
                "channel" to tvDetail.tvChannelName,
                "sourceFrom" to tvDetail.sourceFrom,
                "reason" to reason
            )
        )
    }

    fun logGetListChannel(sourceFrom: String, data: Bundle = bundleOf()) {
        log.logEvent(
            "GetListChannelFrom_$sourceFrom", data
        )
    }

    fun logGetListChannelError(sourceFrom: String, e: Throwable) {
        log.logEvent(
            "Error_GetListChannelFrom_$sourceFrom",
            bundleOf(
                "reason" to (e.message ?: e::class.java.name)
            )
        )
    }

    fun logLoadOpenAds() {
        log.logEvent(
            "loadAdsSuccess", bundleOf(
                "type" to "openApp"
            )
        )
    }

    fun logLoadOpenAdsError() {
        log.logEvent("loadAdsError", bundleOf())
    }

    fun logPictureInPicture(channel: TVChannel) {
        log.logEvent(
            "enterPictureInPictureMode", bundleOf(
                "channel" to channel.tvChannelName,
                "sourceFrom" to channel.sourceFrom
            )
        )
    }

    fun logLoadInterstitialAd(inRetryTime: Int) {
        log.logEvent(
            "LoadInterstitialAd", bundleOf(
                "retryTime" to inRetryTime
            )
        )
    }

    object BannerAds {
        fun logLoadBanner(id: String?, inFragment: Fragment) {
            log.logEvent(
                "AdLoadBanner", bundleOf(
                    "id" to id,
                    "time" to "${System.currentTimeMillis()}"
                )
            )
        }

        fun logLoadBannerFail(id: String?) {
            log.logEvent(
                "AdLoadFail", bundleOf(
                    "id" to id,
                    "time" to "${System.currentTimeMillis()}"
                )
            )
        }

        fun onAdLoaded(id: String?) {
            log.logEvent(
                "AdLoaded", bundleOf(
                    "id" to id,
                    "time" to "${System.currentTimeMillis()}"
                )
            )
        }

        fun logAdClick(id: String?) {
            log.logEvent(
                "AdClick", bundleOf(
                    "id" to id,
                    "time" to "${System.currentTimeMillis()}"
                )
            )
        }

    }
}