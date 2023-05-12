package com.kt.apps.media.mobile.ui.fragments.channels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.VideoDisplayAction
import javax.inject.Inject

class PlaybackViewModel @Inject constructor(): BaseViewModel() {
    enum class State {
        IDLE, LOADING, PLAYING, FINISHED
    }
    val videoSizeStateLiveData: MutableLiveData<VideoSize?> = MutableLiveData(null)
    val videoState: MutableLiveData<State> = MutableLiveData(State.IDLE)
    val isPlayingState: MutableLiveData<Boolean> = MutableLiveData(false)

    val playerListener: Player.Listener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            videoSizeStateLiveData.postValue(videoSize)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when(playbackState) {
                Player.STATE_READY -> {
                    videoState.postValue(State.PLAYING)
                }
                Player.STATE_BUFFERING -> videoState.postValue(State.LOADING)
                Player.STATE_IDLE -> videoState.postValue(State.IDLE)
                Player.STATE_ENDED -> videoState.postValue(State.FINISHED)
                else -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            isPlayingState.postValue(isPlaying)
        }

    }
}