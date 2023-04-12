package com.kt.apps.media.mobile.ui.fragments.channels

import android.os.Bundle
import android.provider.MediaStore.Video
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import cn.pedant.SweetAlert.ProgressHelper
import com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_ALWAYS
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentPlaybackBinding
import com.kt.apps.media.mobile.models.VideoDisplayState
import com.kt.apps.media.mobile.ui.main.TVChannelViewModel
import com.pnikosis.materialishprogress.ProgressWheel
import javax.inject.Inject

class PlaybackFragment : BaseFragment<FragmentPlaybackBinding>() {
    override val layoutResId: Int
        get() = R.layout.fragment_playback
    override val screenName: String
        get() = "Fragment Playback"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManagerMobile

    private val progressHelper by lazy {
        ProgressHelper(this.context)
    }

    //Views
    private val fullScreenButton: ImageButton by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_fullscreen)
    }

    private val controlDock: View by lazy {
        binding.exoPlayer.findViewById(R.id.controller_dock)
    }

    private val progressWheel: ProgressWheel by lazy {
        binding.exoPlayer.findViewById(R.id.progressWheel)
    }

    private val tvChannelViewModel: TVChannelViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[TVChannelViewModel::class.java].apply {
                this.tvWithLinkStreamLiveData.observe(this@PlaybackFragment, linkStreamObserver)
            }
        }
    }

    private val playbackViewModel: PlaybackViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this,factory)[PlaybackViewModel::class.java].apply {
                this.videoIsLoading.observe(this@PlaybackFragment, loadingObserver)
            }
        }
    }

    private val displayState: VideoDisplayState
        get() = playbackViewModel?.videoSizeStateLiveData?.value ?: VideoDisplayState.IDLE

    private val linkStreamObserver: Observer<DataState<TVChannelLinkStream>> by lazy {
        Observer {result ->
            Log.d(TAG, "linkStreamObserver: $result")
            when(result) {
                is DataState.Success -> {
                    playVideo(result.data)
                }
                is DataState.Loading -> {
                    playbackViewModel?.videoSizeStateLiveData?.postValue(VideoDisplayState.LOADING)
                }
                else -> {}
            }
        }
    }

    private val loadingObserver: Observer<Boolean> by lazy {
        Observer { isLoading ->
           if (isLoading) {
               binding.exoPlayer.showController()
               progressHelper.spin()
               progressWheel.visibility = View.VISIBLE
               controlDock.visibility = View.INVISIBLE
           } else {
               progressHelper.stopSpinning()
               progressWheel.visibility = View.INVISIBLE
               controlDock.visibility = View.VISIBLE
           }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(binding) {
            exoPlayer.player = exoPlayerManager.exoPlayer
            exoPlayer.showController()
            exoPlayer.setShowBuffering(SHOW_BUFFERING_ALWAYS)
        }
        fullScreenButton.visibility = View.VISIBLE
        fullScreenButton.setOnClickListener {
            if (this@PlaybackFragment.displayState != VideoDisplayState.FULLSCREEN) {
                playbackViewModel?.changeToFullScreen()
            } else {
                playbackViewModel?.collapseVideo(exoPlayerManager.exoPlayer?.videoSize)
            }

        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        tvChannelViewModel
    }

    private fun playVideo(data: TVChannelLinkStream) {
        exoPlayerManager.playVideo(data.linkStream.map {
            LinkStream(it, data.channel.tvChannelWebDetailPage, data.channel.tvChannelWebDetailPage)
        }, data.channel.isHls, playbackViewModel?.playerListener)
        binding.exoPlayer.player = exoPlayerManager.exoPlayer
    }

}