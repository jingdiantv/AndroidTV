package com.kt.apps.media.xemtv.ui.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.TextView
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.kt.apps.core.GlideApp
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.utils.getHeaderFromLinkStream
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.details.DetailsActivity
import com.kt.apps.media.xemtv.ui.main.MainActivity
import com.kt.skeleton.makeGone
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import kotlin.math.max

/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment(), HasAndroidInjector {

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val tvChannelViewModel: TVChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }
    private val player by lazy {
        ExoPlayer.Builder(requireContext())
            .build()
    }
    private val playerAdapter by lazy {
        LeanbackPlayerAdapter(requireContext(), player, 5)
    }
    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<LeanbackPlayerAdapter>

    private val glueHost by lazy {
        VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
    }

    private var channelAdapter: ObjectAdapter? = null
    private var channelGridViewHolder: VerticalGridPresenter.ViewHolder? = null
    private var channelGridPresenter: VerticalGridPresenter? = null
    private val mChildLaidOutListener =
        OnChildLaidOutListener { _, _, position, _ ->
            if (position == 0) {

            }
        }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this@PlaybackVideoFragment)
        super.onAttach(context)
    }

    private var mCurrentSelectedChannel: TVChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tvChannel =
            activity?.intent?.getParcelableExtra<TVChannelLinkStream>(DetailsActivity.TV_CHANNEL)
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


    class TVChannelPresenter() : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
            val view = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.item_channel_overlay, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
            viewHolder?.view?.findViewById<ImageCardView>(R.id.logoChannel)
                ?.let {
                    GlideApp.with(it)
                        .load((item as TVChannel).logoChannel)
                        .optionalCenterInside()
                        .into(it.mainImageView)
                }
            viewHolder?.view?.findViewById<TextView>(R.id.channel_name)
                ?.text = (item as TVChannel).tvChannelName
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
        }

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
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }

            else -> {
                progressBarManager.hide()
            }
        }
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

    fun onDpadCenter() {
        val visible = channelGridView?.visibility == View.VISIBLE
        if (!visible) {
            channelGridView?.visibility = View.VISIBLE
            setSelectedPosition(max(0, mSelectedPosition))
        }
    }

    fun onDpadLeft() {

    }

    fun onDpadRight() {

    }

    fun onDpadDown() {

    }

    fun onDpadUp() {

    }

    fun onKeyCodeChannelDown() {
        mPlayingPosition = max(0, mPlayingPosition) - 1
        setSelectedPosition(mPlayingPosition)
        Logger.d(this, message = "onKeyCodeChannelDown: $mPlayingPosition")
        if (mPlayingPosition > 0) {
            val item = channelAdapter?.get(mPlayingPosition)
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
        }
    }

    fun onKeyCodeChannelUp() {
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
}