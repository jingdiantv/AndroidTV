package com.kt.apps.core.base.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import javax.inject.Inject

class AudioFocusManager @Inject constructor(
    private val context: Context
) {
    private val audioAttributes by lazy {
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
                }
            }
            .build()
    }

    private val audioFocusChange by lazy {
        AudioManager.OnAudioFocusChangeListener {
            when (it) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {

                }
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    requestFocus(_onFocusChange)
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    releaseFocus()
                    _audioSessionId = null
                    audioRequest = null
                }
            }
        }
    }

    private var _audioSessionId: Int? = null
    private var audioRequest: AudioFocusRequest? = null
    private var _onFocusChange: OnFocusChange? = null

    fun requestFocus(onFocusChange: OnFocusChange? = null) {
        onFocusChange?.let {
            this._onFocusChange = it
        }
        val audioService = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        _audioSessionId ?: return
        _audioSessionId = audioService.generateAudioSessionId()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(audioFocusChange)
                .setAudioAttributes(audioAttributes)
                .build()
            audioService.requestAudioFocus(audioRequest!!)
        } else {
            audioService.requestAudioFocus(
                audioFocusChange,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    fun releaseFocus() {
        _audioSessionId = null
        val audioService = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioRequest?.let {
                audioService.abandonAudioFocusRequest(it)
            }
        } else {
            audioService.abandonAudioFocus(null)
        }
    }

    interface OnFocusChange {
        fun onAudioFocus()
        fun onAudioLossFocus()
    }
}