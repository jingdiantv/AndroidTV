package com.kt.apps.media.mobile.services.media

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.kt.apps.core.base.player.ExoPlayerManagerMobile
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup

class IMediaSessionPreparer(
    private val exoPlayerManagerMobile: ExoPlayerManagerMobile,
    var currentPlayListItem: MutableList<TVChannel>
) : MediaSessionConnector.PlaybackPreparer {
    private var currentItem: TVChannel? = null
    override fun onCommand(player: Player, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean {
        Logger.d(this, message = "onCommand: $command")
        return false
    }

    override fun getSupportedPrepareActions(): Long {
        return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
    }

    override fun onPrepare(playWhenReady: Boolean) {
        Logger.d(this, message = "OnPrepare")
    }

    override fun onPrepareFromMediaId(
        mediaId: String,
        playWhenReady: Boolean,
        extras: Bundle?
    ) {
        Logger.d(this, message = "OnPrepareFromMediaId")
        currentItem = currentPlayListItem.find {
            it.channelId == mediaId
        }
        currentItem?.let {
            exoPlayerManagerMobile.playVideo(
                linkStreams = listOf(LinkStream(
                    it.tvChannelWebDetailPage,
                    it.tvChannelWebDetailPage,
                    it.channelId,
                    it.tvGroup != TVChannelGroup.VOV.name
                )),

                isHls = it.tvGroup != TVChannelGroup.VOV.name,
                itemMetaData = it.getMapData(),
            )
        }
    }

    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
        Logger.d(this, message = "OnPrepareFromSearch")

    }

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {
        Logger.d(this, message = "OnPrepareFromUri: $uri")

    }
}