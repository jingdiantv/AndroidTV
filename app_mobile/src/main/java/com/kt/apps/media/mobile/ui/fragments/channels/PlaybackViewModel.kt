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
    val videoSizeStateLiveData: MutableLiveData<VideoSize?> = MutableLiveData(null)
    val videoIsLoading: MutableLiveData<Boolean> = MutableLiveData(true)

    val playerListener: Player.Listener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            videoSizeStateLiveData.postValue(videoSize)
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
}