package com.kt.apps.media.mobile.services.media

import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.kt.apps.core.tv.model.TVChannel

class IMediaSessionQueueNavigator(
    private val mediaSession: MediaSessionCompat,
    var currentPlayListItem: MutableList<TVChannel>
) : TimelineQueueNavigator(mediaSession) {
    companion object {
        private const val DOUBLE_NEXT_THRESH_HOLD = 300L
    }
    private var lastSkipToNext = System.currentTimeMillis()
    private var doubleNext = false
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    var allowDoubleNext: Boolean = false
    override fun onSkipToNext(player: Player) {
        if (allowDoubleNext) {
            if (System.currentTimeMillis() - lastSkipToNext < DOUBLE_NEXT_THRESH_HOLD) {
                if (!doubleNext) {
                    doubleNext = true
                    handler.removeCallbacksAndMessages(null)
                }
            } else {
                doubleNext = false
                lastSkipToNext = System.currentTimeMillis()
                handler.postDelayed({
                    super.onSkipToNext(player)
                }, DOUBLE_NEXT_THRESH_HOLD)
            }
        } else {
            super.onSkipToNext(player)
        }
    }

    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
        val currentItem = player.currentMediaItem
        if (currentItem != null) {
            return MediaDescriptionCompat.Builder()
                .setTitle(currentItem.mediaMetadata.title)
                .setMediaId(currentItem.mediaId)
                .setExtras(currentItem.mediaMetadata.extras)
                .build()
        }
        return MediaDescriptionCompat.Builder().build()
    }
}