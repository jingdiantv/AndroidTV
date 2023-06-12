package com.kt.apps.media.mobile.ui.fragments.channels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PlaybackFailException
import com.kt.apps.media.mobile.models.VideoDisplayAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class PlaybackViewModel @Inject constructor(): BaseViewModel() {
    sealed class State {
        object IDLE: State()
        object LOADING: State()
        object  PLAYING: State()
        data class FINISHED(val error: Throwable?): State()
    }
    val videoSizeStateLiveData: MutableLiveData<VideoSize?> = MutableLiveData(null)

    private val _state = MutableStateFlow<State>(State.IDLE)
    val state: StateFlow<State> = _state

    val playerListener: Player.Listener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            videoSizeStateLiveData.postValue(videoSize)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            Log.d(TAG, "onPlaybackStateChanged: $playbackState")
            _state.value = when(playbackState) {
                Player.STATE_READY -> State.PLAYING
                Player.STATE_BUFFERING -> State.LOADING
                Player.STATE_ENDED -> State.FINISHED(null)
                Player.STATE_IDLE -> State.IDLE
                else -> _state.value
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

            _state.value  = State.FINISHED(PlaybackFailException(error))
        }
    }
}