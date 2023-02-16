package com.kt.apps.media.xemtv.ui.playback

import android.net.Uri
import android.os.Bundle
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.PlaybackControlsRow
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.utils.getHeaderFromLinkStream
import com.kt.apps.media.xemtv.ui.details.DetailsActivity

/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {
    private val player by lazy {
        ExoPlayer.Builder(requireContext())
            .build()
    }
    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<LeanbackPlayerAdapter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tvChannel =
            activity?.intent?.getParcelableExtra<TVChannelLinkStream>(DetailsActivity.TV_CHANNEL)

        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)

        val playerAdapter = LeanbackPlayerAdapter(requireContext(), player, 5)
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        mTransportControlGlue = PlaybackTransportControlGlue(activity, playerAdapter)
        mTransportControlGlue.host = glueHost
        mTransportControlGlue.title = tvChannel?.channel?.tvChannelName
        mTransportControlGlue.subtitle = tvChannel?.channel?.tvGroup
        mTransportControlGlue.playWhenPrepared()
        val mediaSource = buildMediaSource(tvChannel!!).createMediaSource(
            MediaItem.fromUri(
                Uri.parse(
                    tvChannel.linkStream[0]
                )
            )
        )
        player.setMediaSource(mediaSource)
        player.playWhenReady = true
        playerAdapter.play()
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
        mTransportControlGlue.pause()
    }
}