package com.kt.apps.core.base.player

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.kt.apps.core.R
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.getBaseUrl
import com.kt.apps.core.utils.trustEveryone
import okhttp3.HttpUrl.Companion.toHttpUrl

abstract class AbstractExoPlayerManager(
    private val _application: CoreApp,
    private val _audioFocusManager: AudioFocusManager
) : Application.ActivityLifecycleCallbacks, AudioFocusManager.OnFocusChange {

    protected var mExoPlayer: ExoPlayer? = null

    private val _playerListenerObserver by lazy {
        mutableListOf<(() -> Unit)>()
    }

    private val _audioAttr by lazy {
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

    protected val playerListener by lazy {
        object : Player.Listener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                when (state) {
                    ExoPlayer.STATE_IDLE -> {
                        Logger.d(this@AbstractExoPlayerManager, message = "state: STATE_IDLE")
                    }
                    ExoPlayer.STATE_BUFFERING -> {
                        Logger.d(this@AbstractExoPlayerManager, message = "state: STATE_BUFFERING")
                    }
                    ExoPlayer.STATE_READY -> {
                        Logger.d(this@AbstractExoPlayerManager, message = "state: STATE_READY")
                    }
                    ExoPlayer.STATE_ENDED -> {
                        Logger.d(this@AbstractExoPlayerManager, message = "state: STATE_ENDED")
                    }
                    else -> {
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                mExoPlayer = null
                Logger.d(
                    this@AbstractExoPlayerManager,
                    message = error.message?.plus(error.errorCodeName) ?: error.errorCodeName
                )
            }

        }
    }

    init {
        _application.registerActivityLifecycleCallbacks(this)
    }


    val exoPlayer: ExoPlayer?
        get() = mExoPlayer

    fun addListener() {
        _playerListenerObserver.add {
        }
    }

    open fun prepare() {
        mExoPlayer?.stop()
        mExoPlayer?.release()
        mExoPlayer = buildExoPlayer()
    }

    protected fun buildExoPlayer() = ExoPlayer.Builder(_application)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setAudioAttributes(_audioAttr, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    fun pause() {
        Logger.d(this, message = "Pause")
        mExoPlayer?.pause()
    }

    fun getMediaSource(
        data: List<LinkStream>
    ): List<MediaSource> {
        return data.map {
            val dfSource: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
            dfSource.setDefaultRequestProperties(
                getHeader90pLink(data.first().referer, data.first())
            )
            dfSource.setKeepPostFor302Redirects(true)
            dfSource.setAllowCrossProtocolRedirects(true)
            dfSource.setUserAgent(_application.getString(R.string.user_agent))
            if (it.isHls) {
                DefaultMediaSourceFactory(dfSource)
                    .setServerSideAdInsertionMediaSourceFactory(DefaultMediaSourceFactory(dfSource))
                    .createMediaSource(MediaItem.fromUri(it.m3u8Link.trim()))
            } else {
                ProgressiveMediaSource.Factory(dfSource)
                    .createMediaSource(MediaItem.fromUri(it.m3u8Link.trim()))
            }
        }
    }

    open fun getMediaSource(
        data: List<LinkStream>,
        isHls: Boolean
    ): List<MediaSource> {
        val dfSource: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        dfSource.setDefaultRequestProperties(
            getHeader90pLink(data.first().referer, data.first())
        )
        dfSource.setKeepPostFor302Redirects(true)
        dfSource.setAllowCrossProtocolRedirects(true)
        dfSource.setUserAgent(_application.getString(R.string.user_agent))
        return data.map { it.m3u8Link }.map {
            if (isHls) {
                DefaultMediaSourceFactory(dfSource)
                    .setServerSideAdInsertionMediaSourceFactory(DefaultMediaSourceFactory(dfSource))
                    .createMediaSource(MediaItem.fromUri(it.trim()))
            } else {
                ProgressiveMediaSource.Factory(dfSource)
                    .createMediaSource(MediaItem.fromUri(it.trim()))
            }
        }
    }

    open fun playVideo(
        data: List<LinkStream>,
        isHls: Boolean,
        playerListener: Player.Listener? = null
    ) {
        prepare()
        trustEveryone()
        val mediaSources = getMediaSource(data, isHls)
        mExoPlayer?.setMediaSources(mediaSources)
        mExoPlayer?.removeListener(this.playerListener)
        mExoPlayer?.addListener(this.playerListener)
        playerListener?.let {
            mExoPlayer?.removeListener(it)
            mExoPlayer?.addListener(it)
        }
        mExoPlayer?.playWhenReady = true
        mExoPlayer?.prepare()
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

    abstract fun detach(listener: Player.Listener? = null)


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        if (activity::class.java.name.contains("PlaybackActivity")) {
            mExoPlayer?.play()
        }
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity::class.java.name.contains("PlaybackActivity")) {
            mExoPlayer?.stop()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onAudioFocus() {
    }

    override fun onAudioLossFocus() {
        mExoPlayer?.pause()
    }


}