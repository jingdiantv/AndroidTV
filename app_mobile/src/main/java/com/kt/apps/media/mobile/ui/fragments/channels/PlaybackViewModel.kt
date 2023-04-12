package com.kt.apps.media.mobile.ui.fragments.channels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.models.VideoDisplayState
import javax.inject.Inject

class PlaybackViewModel @Inject constructor(): BaseViewModel() {
    val videoSizeStateLiveData: MutableLiveData<VideoDisplayState> = MutableLiveData(VideoDisplayState.IDLE)
    val videoIsLoading: MutableLiveData<Boolean> = MutableLiveData(true)

    val playerListener: Player.Listener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSizeStateLiveData.value == VideoDisplayState.FULLSCREEN) {
                return
            }
            videoSizeStateLiveData.postValue(VideoDisplayState.SUCCESS(videoSize))
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when(playbackState) {
                Player.STATE_READY -> videoIsLoading.postValue(false)
                Player.STATE_BUFFERING -> videoIsLoading.postValue(true)
                else -> videoIsLoading.postValue(true)
            }
        }

    }

    fun changeToFullScreen() {
        videoSizeStateLiveData.postValue(VideoDisplayState.FULLSCREEN)
    }

    fun collapseVideo(videoSize: VideoSize?) {
        videoSize?.let {
            videoSizeStateLiveData.postValue(VideoDisplayState.SUCCESS(it))
        } ?: run {
            videoSizeStateLiveData.postValue(VideoDisplayState.LOADING)
        }

    }
}