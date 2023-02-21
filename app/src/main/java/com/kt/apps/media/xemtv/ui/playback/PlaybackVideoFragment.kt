package com.kt.apps.media.xemtv.ui.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
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
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

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
    val glueHost by lazy {
        VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this@PlaybackVideoFragment)
        super.onAttach(context)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tvChannel =
            activity?.intent?.getParcelableExtra<TVChannelLinkStream>(DetailsActivity.TV_CHANNEL)


        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        mTransportControlGlue = PlaybackTransportControlGlue(activity, playerAdapter)

        tvChannel?.let {
            mTransportControlGlue.host = glueHost
            mTransportControlGlue.title = tvChannel.channel.tvChannelName
            mTransportControlGlue.subtitle = tvChannel.channel.tvGroup
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
                val gridPresenter = ChannelItemPresenter()
                val gridRowAdapter = ArrayObjectAdapter(gridPresenter)
                gridRowAdapter.setItems(dataState.data, object : DiffCallback<TVChannel>() {
                    override fun areItemsTheSame(oldItem: TVChannel, newItem: TVChannel): Boolean {
                        return oldItem.channelId.equals(newItem.channelId, ignoreCase = true)
                    }

                    override fun areContentsTheSame(oldItem: TVChannel, newItem: TVChannel): Boolean {
                        return oldItem.tvChannelWebDetailPage == newItem.tvChannelWebDetailPage
                                && oldItem.tvChannelName == newItem.tvChannelName
                                && oldItem.sourceFrom == newItem.sourceFrom
                    }
                })
                adapter = gridRowAdapter
            }
            is DataState.Error -> {

            }
            is DataState.Loading -> {

            }
            else -> {

            }
        }
    }

    private inner class ChannelItemPresenter : Presenter() {

        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
            val view = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.item_menu_channel, parent, false)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            return ChannelItemViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
            if (item is TVChannel) {
                (viewHolder as ChannelItemViewHolder).apply {
                    GlideApp.with(logoImgView)
                        .load(item.logoChannel)
                        .into(logoImgView.mainImageView)
                    setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->

                    }
                }
            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
        }

        inner class ChannelItemViewHolder(view: View) : ViewHolder(view) {
            val logoImgView: ImageCardView by lazy {
                view.findViewById(R.id.logoChannel)
            }

            val channelNameTxtView: TextView by lazy {
                view.findViewById(R.id.channel_name)
            }
        }

    }

    private fun streamingByDataState(dataState: DataState<TVChannelLinkStream>?) {
        when (dataState) {
            is DataState.Success -> {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root: ViewGroup = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        return root
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

    override fun onPause() {
        super.onPause()
//        mTransportControlGlue.pause()
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return injector
    }
}