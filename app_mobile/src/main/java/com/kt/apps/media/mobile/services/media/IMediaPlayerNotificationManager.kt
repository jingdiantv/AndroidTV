package com.kt.apps.media.mobile.services.media

import android.content.Context
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.kt.apps.media.mobile.R

const val NOW_PLAYING_CHANNEL_ID = "iMedia"
const val NOW_PLAYING_NOTIFICATION_ID = 1234
//
//class IMediaPlayerNotificationManager(
//    private val _context: Context,
//    channelId: String = NOW_PLAYING_CHANNEL_ID,
//    notificationId: Int = NOW_PLAYING_NOTIFICATION_ID,
//    private val mediaDescriptionAdapter: MediaDescriptionAdapter?,
//    notificationListener: NotificationListener?,
//    private val _customActionReceiver: CustomActionReceiver?,
//) : PlayerNotificationManager(
//    _context,
//    channelId,
//    notificationId,
//    mediaDescriptionAdapter!!,
//    notificationListener,
//    _customActionReceiver,
//    R.drawable.ic_notification,
//    R.drawable.round_play_arrow_24,
//    R.drawable.outline_pause_circle_24,
//    R.drawable.round_stop_24,
//    R.drawable.round_fast_rewind_24,
//    R.drawable.round_fast_forward_24,
//    R.drawable.round_mic_24,
//    R.drawable.round_skip_next_24,
//    null
//) {
//
//    override fun getOngoing(player: Player): Boolean {
//        return super.getOngoing(player)
//    }
//
//    override fun getActions(player: Player): List<String> {
//        return super.getActions(player)
//    }
//
//}