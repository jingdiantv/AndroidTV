package com.kt.apps.media.mobile.ui.fragments.channels

import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.logPlaybackError
import com.kt.apps.core.logging.logPlaybackShowError
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.PlaybackFailException
import com.kt.apps.media.mobile.models.VideoDisplayAction
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class PlaybackViewModel @Inject constructor(): BaseViewModel() {
    sealed class State {
        object IDLE: State()
        object LOADING: State()
        object  PLAYING: State()
        data class FINISHED(val error: Throwable?): State()
    }

    @Inject
    lateinit var actionLogger: IActionLogger

    val videoSizeStateLiveData: MutableLiveData<VideoSize?> = MutableLiveData(null)

    private val _state = MutableStateFlow<State>(State.IDLE)
    val state: StateFlow<State> = _state

    private val _displayState = MutableStateFlow(PlaybackState.Fullscreen)
    val displayState: StateFlow<PlaybackState> = _displayState


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

    fun changeDisplayState(newMode: PlaybackState) {
        _displayState.value = newMode
    }
}