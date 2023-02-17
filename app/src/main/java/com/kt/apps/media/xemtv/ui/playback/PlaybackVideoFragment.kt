package com.kt.apps.media.xemtv.ui.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.PlaybackControlsRow
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.utils.getHeaderFromLinkStream
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
            when(it) {
                is DataState.Success -> {
                    progressBarManager.hide()
                    val tvChannel = it.data.channel
                    val linkStream = it.data.linkStream
                    playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

                    mTransportControlGlue = PlaybackTransportControlGlue(activity, playerAdapter)
                    mTransportControlGlue.host = glueHost
                    mTransportControlGlue.title = tvChannel.tvChannelName
                    mTransportControlGlue.subtitle = tvChannel.tvGroup
                    mTransportControlGlue.playWhenPrepared()
                    val mediaSource = buildMediaSource(it.data).createMediaSource(
                        MediaItem.fromUri(
                            Uri.parse(
                                linkStream[0]
                            )
                        )
                    )
                    player.setMediaSource(mediaSource)
                    player.playWhenReady = true
                    Handler(Looper.getMainLooper()).post {
                        playerAdapter.play()
                    }
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

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root: ViewGroup = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
//        root.addView()
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