package com.kt.apps.media.xemtv.ui.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.IKeyCodeHandler
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.base.player.ExoPlayerManager
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.utils.getHeaderFromLinkStream
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.details.DetailsActivity
import com.kt.apps.media.xemtv.ui.main.MainActivity
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.skeleton.makeGone
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import kotlin.math.max

/** Handles video playback with media controls. */
class TVPlaybackVideoFragment : VideoSupportFragment(), HasAndroidInjector, IKeyCodeHandler {

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val tvChannelViewModel: TVChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }
    private val player by lazy {
        ExoPlayer.Builder(requireContext())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_NONE)
                        }
                    }
                    .build(), true)
            .build()
    }
    private val playerAdapter by lazy {
        LeanbackPlayerAdapter(requireContext(), player, 5)
    }
    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<LeanbackPlayerAdapter>

    private val glueHost by lazy {
        VideoSupportFragmentGlueHost(this@TVPlaybackVideoFragment)
    }

    private var channelAdapter: ObjectAdapter? = null
    private var channelGridViewHolder: VerticalGridPresenter.ViewHolder? = null
    private var channelGridPresenter: VerticalGridPresenter? = null
    private val mChildLaidOutListener =
        OnChildLaidOutListener { _, _, position, _ ->
            if (position == 0) {

            }
        }
    private val _playerListener by lazy {
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                retryGetLinkStream()
            }
        }
    }

    private fun retryGetLinkStream() {
        tvChannelViewModel.retryGetLastWatchedChannel()
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this@TVPlaybackVideoFragment)
        super.onAttach(context)
    }

    private var mCurrentSelectedChannel: TVChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tvChannel = activity?.intent
            ?.getParcelableExtra<TVChannelLinkStream>(DetailsActivity.TV_CHANNEL)
        tvChannelViewModel.markLastWatchedChannel(tvChannel)
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        mTransportControlGlue = PlaybackTransportControlGlue(activity, playerAdapter)

        tvChannel?.let {
            mCurrentSelectedChannel = it.channel
            mTransportControlGlue.host = glueHost
            mTransportControlGlue.title = tvChannel.channel.tvChannelName
            mTransportControlGlue.subtitle = tvChannel.channel.tvGroup
            mTransportControlGlue.isSeekEnabled = false
            mTransportControlGlue.playWhenPrepared()
            val mediaSource = buildMediaSource(tvChannel).createMediaSource(
                MediaItem.fromUri(
                    Uri.parse(
                        tvChannel.linkStream[0]
                    )
                )
            )
            player.setMediaSource(mediaSource)
            player.playWhenReady = true
            player.addListener(_playerListener)
            playerAdapter.play()
        } ?: let {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root: ViewGroup = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        initView(container, root)
        return root
    }

    private fun initView(container: ViewGroup?, root: ViewGroup) {
        val gridView = LayoutInflater.from(context)
            .inflate(R.layout.tv_channel_grid_view_overlay, container, false)
        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        gridView.layoutParams = layoutParams
        setupChannelListView(gridView)
        root.addView(gridView)
        hideControlsOverlay(false)
        val controlBackground = root.findViewById<View>(androidx.leanback.R.id.playback_controls_dock)
        controlBackground.makeGone()
    }

    private var channelGridView: View? = null
    private fun setupChannelListView(gridView: View) {
        channelGridView = gridView
        val gridDock = gridView.findViewById<FrameLayout>(R.id.browse_grid_dock)
        channelGridPresenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM)
        channelGridPresenter!!.numberOfColumns = 5
        channelGridViewHolder = channelGridPresenter!!.onCreateViewHolder(gridDock)
        gridDock.addView(channelGridViewHolder!!.view)
        channelGridPresenter?.setOnItemViewSelectedListener { _, _, _, _ ->
            val position = channelGridViewHolder?.gridView?.selectedPosition ?: 0
            mSelectedPosition = position
        }
        channelGridPresenter?.setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
            (channelAdapter as ArrayObjectAdapter).indexOf(item)
                .takeIf {
                    it > -1
                }?.let {
                    mSelectedPosition = it
                }
        }
        channelGridViewHolder!!.gridView.setOnChildLaidOutListener(mChildLaidOutListener)
        channelGridPresenter!!.onBindViewHolder(channelGridViewHolder, channelAdapter)
        gridView.makeGone()
    }

    private var mSelectedPosition: Int = 0
    private var mPlayingPosition: Int = 0
    private fun updateAdapter() {
        if (channelGridViewHolder != null) {
            channelGridPresenter!!.onBindViewHolder(channelGridViewHolder, channelAdapter)
            if (mSelectedPosition != -1) {
                setSelectedPosition(max(0, mSelectedPosition))
            }
        }
    }

    private fun setupRowAdapter(tvChannelList: List<TVChannel>) {
        mSelectedPosition = mCurrentSelectedChannel?.let {
            val lastChannel = tvChannelList.findLast {
                it.channelId == mCurrentSelectedChannel!!.channelId
            }
            tvChannelList.lastIndexOf(lastChannel)
        } ?: 0
        mPlayingPosition = mSelectedPosition
        Logger.d(this, message = "setupRowAdapter: $mSelectedPosition")
        val cardPresenterSelector: PresenterSelector = TVChannelPresenterSelector(requireActivity())
        val mAdapter = ArrayObjectAdapter(cardPresenterSelector)
        mAdapter.addAll(0, tvChannelList)
        channelAdapter = mAdapter
        updateAdapter()
    }

    override fun setSelectedPosition(position: Int) {
        mSelectedPosition = position
        channelGridViewHolder?.gridView?.setSelectedPositionSmooth(position)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvChannelViewModel.tvWithLinkStreamLiveData.observe(viewLifecycleOwner) {
            streamingByDataState(it)
        }

        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            loadChannelListByDataState(it)
        }
    }

    private fun loadChannelListByDataState(dataState: DataState<List<TVChannel>>) {
        when (dataState) {
            is DataState.Success -> {
                setupRowAdapter(dataState.data)
                if (mPlayingPosition <= 0 && mCurrentSelectedChannel != null) {
                    mPlayingPosition = dataState.data.indexOfLast {
                        it.channelId == mCurrentSelectedChannel!!.channelId
                    }.takeIf {
                        it >= 0
                    } ?: 0
                }
            }
            is DataState.Error -> {

            }
            is DataState.Loading -> {

            }
            else -> {

            }
        }
    }

    private fun streamingByDataState(dataState: DataState<TVChannelLinkStream>?) {
        when (dataState) {
            is DataState.Success -> {
                mCurrentSelectedChannel = dataState.data.channel
                channelGridView?.makeGone()
                progressBarManager.hide()
                val tvChannel = dataState.data.channel
                val linkStream = dataState.data.linkStream
                playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)
                mTransportControlGlue = PlaybackTransportControlGlue(activity, playerAdapter)
                mTransportControlGlue.host = glueHost
                mTransportControlGlue.title = tvChannel.tvChannelName
                mTransportControlGlue.subtitle = tvChannel.tvGroup
                mTransportControlGlue.playWhenPrepared()
                val mediaSource = buildMediaSource(dataState.data).createMediaSource(
                    MediaItem.fromUri(
                        Uri.parse(
                            linkStream[0]
                        )
                    )
                )
                player.setMediaSource(mediaSource)
                player.playWhenReady = true
                playerAdapter.play()
                Logger.d(this, message = "Play media source")
            }

            is DataState.Loading -> {
                progressBarManager.show()
            }

            is DataState.Error -> {
                progressBarManager.hide()

                showErrorDialog(content = dataState.throwable.message,
                    onSuccessListener = {
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    })
            }

            else -> {
                progressBarManager.hide()
            }
        }
    }

    override fun onDetach() {
        progressBarManager.hide()
        exoPlayerManager.pause()
        Logger.d(this, message = "onDetach")
        super.onDetach()
    }

    override fun onStop() {
        player.stop()
        super.onStop()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    private fun buildMediaSource(tvChannelDetail: TVChannelLinkStream): DefaultMediaSourceFactory {
        val dfSource: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        dfSource.setDefaultRequestProperties(
            getHeaderFromLinkStream(
                tvChannelDetail.channel.tvChannelWebDetailPage, if (tvChannelDetail.channel
                        .sourceFrom == TVDataSourceFrom.VTC_BACKUP.name
                ) tvChannelDetail.channel.tvChannelWebDetailPage else ""
            )
        )
        return DefaultMediaSourceFactory(dfSource)
            .setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
            })

    }

    override fun onDpadCenter() {
        val visible = channelGridView?.visibility == View.VISIBLE
        if (!visible) {
            channelGridView?.visibility = View.VISIBLE
            setSelectedPosition(max(0, mSelectedPosition))
        }
    }

    override fun onDpadLeft() {

    }

    override fun onDpadRight() {

    }

    override fun onDpadDown() {

    }

    override fun onDpadUp() {

    }

    override fun onKeyCodeChannelDown() {
        mPlayingPosition = max(0, mPlayingPosition) - 1
        setSelectedPosition(mPlayingPosition)
        Logger.d(this, message = "onKeyCodeChannelDown: $mPlayingPosition")
        if (mPlayingPosition > 0) {
            val item = channelAdapter?.get(mPlayingPosition)
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
        }
    }

    override fun onKeyCodeMediaPrevious() {

    }

    override fun onKeyCodeMediaNext() {
    }

    override fun onKeyCodeVolumeUp() {
    }

    override fun onKeyCodeVolumeDown() {
    }

    override fun onKeyCodePause() {
    }

    override fun onKeyCodePlay() {
    }

    override fun onKeyCodeChannelUp() {
        mPlayingPosition = max(0, mPlayingPosition) + 1

        setSelectedPosition(mPlayingPosition)
        Logger.d(this, message = "onKeyCodeChannelUp: $mPlayingPosition")
        val maxItemCount = channelGridViewHolder?.gridView?.adapter?.itemCount ?: 0
        if (mPlayingPosition <= maxItemCount - 1) {
            val item = channelAdapter?.get(mPlayingPosition)
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
        }
    }

    fun canBackToMain(): Boolean {
        return channelGridView?.visibility != View.VISIBLE
    }

    fun hideChannelMenu() {
        channelGridView?.makeGone()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return injector
    }

    companion object {
        fun newInstance(type: PlaybackActivity.Type): TVPlaybackVideoFragment {
            return TVPlaybackVideoFragment()
        }
    }
}