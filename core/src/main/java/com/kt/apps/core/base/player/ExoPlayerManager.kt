package com.kt.apps.core.base.player

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.kt.apps.core.R
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.utils.trustEveryone
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject


class ExoPlayerManager @Inject constructor(
    private val application: CoreApp,
    private val audioFocusManager: AudioFocusManager
) : Application.ActivityLifecycleCallbacks, AudioFocusManager.OnFocusChange {
    private var exoPlayer: ExoPlayer? = null
    private var playerAdapter: LeanbackPlayerAdapter? = null
    private var isFullScreen: Boolean = false

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    private val audioAttr by lazy {
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_NONE)
                }
            }
            .build()
    }

    private val playerListener by lazy {
        object : Player.Listener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                when (state) {
                    ExoPlayer.STATE_IDLE -> {
                    }
                    ExoPlayer.STATE_BUFFERING -> {
                    }
                    ExoPlayer.STATE_READY -> {
                    }
                    ExoPlayer.STATE_ENDED -> {
                    }
                    else -> {
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
            }

        }
    }

    private fun updateFullScreenState(isFullScreen: Boolean) {
        this.isFullScreen = isFullScreen
    }

    fun playVideo(data: List<LinkStream>) {
        audioFocusManager.requestFocus(this)
        if (exoPlayer == null) {
            exoPlayer = buildExoPlayer()
            playerAdapter = LeanbackPlayerAdapter(application, exoPlayer!!, 5)
        }
        val dfSource: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        dfSource.setDefaultRequestProperties(
            getHeader90pLink(data.first().referer, data.first())
        )
        dfSource.setKeepPostFor302Redirects(true)
        dfSource.setAllowCrossProtocolRedirects(true)
        dfSource.setUserAgent(application.getString(R.string.user_agent))
        trustEveryone()

        val mediaSources = data.map { it.m3u8Link }.map {
            DefaultMediaSourceFactory(dfSource)
                .setServerSideAdInsertionMediaSourceFactory(DefaultMediaSourceFactory(dfSource))
                .createMediaSource(MediaItem.fromUri(it.trim()))
        }

        exoPlayer?.setMediaSources(mediaSources)
        exoPlayer?.addListener(playerListener)
        exoPlayer?.playWhenReady = true
        exoPlayer?.prepare()
        playerAdapter?.play()
    }

    private fun buildExoPlayer() = ExoPlayer.Builder(application)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .setAudioAttributes(audioAttr, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    fun pause() {
        playerAdapter?.pause()
    }

    private fun getHeader90pLink(referer: String, currentLinkStream: LinkStream): Map<String, String> {
        val needHost = referer.contains("auth_key")
        val host = try {
            referer.trim().toHttpUrl().host
        } catch (e: Exception) {
            ""
        }
        return mutableMapOf(
            "Accept" to "*/*",
            "accept-encoding" to "gzip, deflate, br",
            "origin" to referer.getBaseUrl(),
            "referer" to referer.trim(),
            "sec-fetch-dest" to "empty",
            "sec-fetch-site" to "cross-site",
            "user-agent" to application.getString(R.string.user_agent),
        ).apply {
            if (needHost) {
                this["Host"] = host
            }
            currentLinkStream.token?.let {
                this["token"] = it
                this["Authorization"] = it
            }
            currentLinkStream.host?.let {
                this["host"] = host
            }
        }
    }

    private fun String.getBaseUrl(): String {
        val isUrl = this.trim().startsWith("http")
        val isHttps = this.trim().startsWith("https")
        if (!isUrl) return ""
        val baseUrl = this.toHttpUrl().host
        return if (isHttps) "https://${baseUrl.trim()}" else "http://${baseUrl.trim()}"
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        exoPlayer?.play()
    }

    override fun onActivityPaused(activity: Activity) {
        exoPlayer?.pause()
    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onAudioFocus() {
    }

    override fun onAudioLossFocus() {
        playerAdapter?.pause()
    }

}