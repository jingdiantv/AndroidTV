package com.kt.apps.media.mobile.ui.fragments.channels

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.*
import cn.pedant.SweetAlert.ProgressHelper
import com.google.android.exoplayer2.ui.StyledPlayerView.ControllerVisibilityListener
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.FragmentPlaybackBinding
import com.kt.apps.media.mobile.ui.complex.PlaybackState
import com.kt.apps.media.mobile.ui.fragments.dialog.JobQueue
import com.kt.apps.media.mobile.ui.fragments.lightweightchannels.LightweightChannelFragment
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.utils.ktFadeIn
import com.kt.apps.media.mobile.utils.ktFadeOut
import com.pnikosis.materialishprogress.ProgressWheel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

interface IPlaybackAction {
    fun onLoadedSuccess(videoSize: VideoSize)
    fun onOpenFullScreen()

    fun onPauseAction(userAction: Boolean)
    fun onPlayAction(userAction: Boolean)
}

class PlaybackFragment : BaseFragment<FragmentPlaybackBinding>() {

    override val layoutResId: Int
        get() = R.layout.fragment_playback
    override val screenName: String
        get() = "Fragment Playback"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManagerMobile

    var callback: IPlaybackAction? = null
    private var _cachePlayingState: Boolean = false

    private var _displayState: PlaybackState = PlaybackState.Invisible
    var displayState: PlaybackState
        get() = _displayState
        set(value) {
            _displayState = value
            if (value != PlaybackState.Fullscreen) {
                showHideChannelList(false)
            }
        }

    private val progressHelper by lazy {
        ProgressHelper(this.context)
    }

    //Views
    private val fullScreenButton: ImageButton by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_fullscreen)
    }

    private val playPauseButton: ImageButton by lazy {
        binding.exoPlayer.findViewById(com.google.android.exoplayer2.ui.R.id.exo_play_pause)
    }

    private val progressWheel: ProgressWheel by lazy {
        binding.exoPlayer.findViewById(R.id.progressWheel)
    }

    private val titleLabel: TextView by lazy {
        binding.exoPlayer.findViewById(R.id.title_player)
    }

    private val channelFragmentContainer: FragmentContainerView by lazy {
        binding.exoPlayer.findViewById(R.id.player_overlay_container)
    }

    private val isProcessing by lazy {
        MutableStateFlow<Boolean>(false)
    }

    private var shouldShowChannelList: Boolean = false


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
                this.videoSizeStateLiveData.observe(this@PlaybackFragment) {videoSize ->
                    videoSize?.run {
                        callback?.onLoadedSuccess(this)
                    }
                }
            }
        }
    }

    private val linkStreamObserver: Observer<DataState<TVChannelLinkStream>> by lazy {
        Observer {result ->
            Log.d(TAG, "linkStreamObserver: $result")
            when(result) {
                is DataState.Success -> {
                    toggleProgressing(true)
                    playVideo(result.data)
                }
                is DataState.Loading -> {
                    stopCurrentVideo()
                    toggleProgressing(true)
                    showHideChannelList(false)
                }
                else -> {}
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(binding) {
            exoPlayer.player = exoPlayerManager.exoPlayer
            exoPlayer.showController()
            exoPlayer.setShowNextButton(false)
            exoPlayer.setShowPreviousButton(false)
            exoPlayer.setControllerVisibilityListener(ControllerVisibilityListener { visibility ->
                if (visibility != View.VISIBLE)
                    channelFragmentContainer.visibility = visibility
                else
                    if (shouldShowChannelList)
                        showHideChannelList(isShow = true)
            })
        }

        fullScreenButton.visibility = View.VISIBLE
        fullScreenButton.setOnClickListener {
            callback?.onOpenFullScreen()
        }

        playPauseButton.setOnClickListener {
            exoPlayerManager.exoPlayer?.run {
                shouldShowChannelList = if (isPlaying) {
                    pause()
                    showHideChannelList(isShow = true)
                    true
                } else {
                    play()
                    showHideChannelList(isShow = false)
                    false
                }
            }
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
        tvChannelViewModel

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playbackViewModel?.state?.map {
                        it == PlaybackViewModel.State.LOADING
                    }?.collectLatest {
                        toggleProgressing(it)
                    }
                }

                launch {
                    isProcessing
                        .collectLatest {
                            if (it) {
                                animationQueue.submit(kotlin.coroutines.coroutineContext) {
                                    binding.exoPlayer.showController()
                                    progressWheel.visibility = View.VISIBLE
                                    awaitAll(async {
                                        progressWheel.ktFadeIn()
                                        progressHelper.spin()
                                    })
                                }
                            } else {
                                animationQueue.submit(kotlin.coroutines.coroutineContext) {
                                    awaitAll(async {
                                        progressWheel.ktFadeOut()
                                        progressHelper.stopSpinning()
                                    })
                                }
                            }
                        }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        _cachePlayingState = exoPlayerManager.exoPlayer?.isPlaying ?: false
        exoPlayerManager.pause()
        shouldShowChannelList = false
    }

    override fun onResume() {
        super.onResume()
        shouldShowChannelList = false
        _cachePlayingState = if (_cachePlayingState) {
            exoPlayerManager.exoPlayer?.play()
            false
        } else false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        childFragmentManager.beginTransaction()
            .replace(R.id.player_overlay_container, LightweightChannelFragment.newInstance())
            .commit()

        channelFragmentContainer.visibility = View.INVISIBLE
    }

    private fun playVideo(data: TVChannelLinkStream) {
        exoPlayerManager.playVideo(data.linkStream.map {
            LinkStream(it, data.channel.tvChannelWebDetailPage, data.channel.tvChannelWebDetailPage)
        }, data.channel.isHls, playbackViewModel?.playerListener)
        binding.exoPlayer.player = exoPlayerManager.exoPlayer
        activity?.runOnUiThread {
            titleLabel.text = data.channel.tvChannelName
        }
    }

    private fun stopCurrentVideo() {
        exoPlayerManager.exoPlayer?.stop()
    }

    private val animationQueue: JobQueue by lazy {
        JobQueue()
    }
    private fun toggleProgressing(isShow: Boolean) {
        isProcessing.tryEmit(isShow)
    }

    private fun showHideChannelList(isShow: Boolean) {
        if (isShow && displayState == PlaybackState.Fullscreen) {
            channelFragmentContainer.fadeIn {  }
            return
        }
        if (!isShow && displayState == PlaybackState.Fullscreen) {
            channelFragmentContainer.fadeOut {  }
            return
        }
        channelFragmentContainer.visibility = View.INVISIBLE
    }
}