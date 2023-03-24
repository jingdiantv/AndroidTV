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
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.trustEveryone
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject


class ExoPlayerManagerMobile @Inject constructor(
    private val _application: CoreApp,
    private val _audioFocusManager: AudioFocusManager
) : Application.ActivityLifecycleCallbacks, AudioFocusManager.OnFocusChange {
    private var _exoPlayer: ExoPlayer? = null
    private val _playerListenerObserver by lazy {
        mutableListOf<(() -> Unit)>()
    }

    init {
        _application.registerActivityLifecycleCallbacks(this)
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

    private val _playerListener by lazy {
        object : Player.Listener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                when (state) {
                    ExoPlayer.STATE_IDLE -> {
                        Logger.d(this@ExoPlayerManagerMobile, message = "state: STATE_IDLE")
                    }
                    ExoPlayer.STATE_BUFFERING -> {
                        Logger.d(this@ExoPlayerManagerMobile, message = "state: STATE_BUFFERING")
                    }
                    ExoPlayer.STATE_READY -> {
                        Logger.d(this@ExoPlayerManagerMobile, message = "state: STATE_READY")
                    }
                    ExoPlayer.STATE_ENDED -> {
                        Logger.d(this@ExoPlayerManagerMobile, message = "state: STATE_ENDED")
                    }
                    else -> {
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                _exoPlayer = null
                Logger.d(this@ExoPlayerManagerMobile, message = error.message?.plus(error.errorCodeName) ?: error.errorCodeName)
            }

        }
    }

    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

    fun addListener() {
        _playerListenerObserver.add {
        }
    }

    fun prepare() {
        _exoPlayer?.stop()
        _exoPlayer?.release()
        _exoPlayer = buildExoPlayer()
    }

    fun playVideo(
        data: List<LinkStream>,
        playerListener: Player.Listener? = null
    ) {
        if (_exoPlayer == null) {
            _exoPlayer = buildExoPlayer()
        }
        _exoPlayer?.removeListener(_playerListener)
        val dfSource: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        dfSource.setDefaultRequestProperties(
            getHeader90pLink(data.first().referer, data.first())
        )
        dfSource.setKeepPostFor302Redirects(true)
        dfSource.setAllowCrossProtocolRedirects(true)
        dfSource.setUserAgent(_application.getString(R.string.user_agent))
        trustEveryone()

        val mediaSources = data.map { it.m3u8Link }.map {
            DefaultMediaSourceFactory(dfSource)
                .setServerSideAdInsertionMediaSourceFactory(DefaultMediaSourceFactory(dfSource))
                .createMediaSource(MediaItem.fromUri(it.trim()))
        }

        _exoPlayer?.setMediaSources(mediaSources)
        _exoPlayer?.addListener(_playerListener)
        playerListener?.let {
            _exoPlayer?.removeListener(it)
            _exoPlayer?.addListener(it)
        }
        _exoPlayer?.playWhenReady = true
        _exoPlayer?.prepare()
        _exoPlayer?.play()

    }

    private fun buildExoPlayer() = ExoPlayer.Builder(_application)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setAudioAttributes(audioAttr, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    fun pause() {
        Logger.d(this, message = "Pause")
        _exoPlayer?.pause()
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
            "user-agent" to _application.getString(R.string.user_agent),
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
        if (activity::class.java.name.contains("PlaybackActivity")) {
            _exoPlayer?.play()
        }
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity::class.java.name.contains("PlaybackActivity")) {
            _exoPlayer?.stop()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onAudioFocus() {
    }

    override fun onAudioLossFocus() {
        _exoPlayer?.pause()
    }

    fun detach(listener: Player.Listener? = null) {
        if (listener != null) {
            _exoPlayer?.removeListener(listener)
        }
        _exoPlayer?.removeListener(_playerListener)
        _audioFocusManager.releaseFocus()
        _exoPlayer?.release()
        _exoPlayer = null
    }

}