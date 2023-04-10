package com.kt.apps.media.mobile.services.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

import com.kt.apps.core.logging.Logger

class IMusicServiceConnection(context: Context, serviceComponent: ComponentName) {
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var mediaControllerCallback: MediaControllerCompat.Callback
    private lateinit var mediaBrowserConnectionCallback: MediaBrowserCompat.ConnectionCallback
    private val mediaBrowser by lazy {
        MediaBrowserCompat(
            context,
            serviceComponent,
            mediaBrowserConnectionCallback, null
        ).apply { connect() }
    }

    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls


    init {
        mediaControllerCallback = object : MediaControllerCompat.Callback() {

            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {

            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {

            }

            override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            }

            override fun onSessionEvent(event: String?, extras: Bundle?) {
                super.onSessionEvent(event, extras)
                Logger.d(this@IMusicServiceConnection, message = event ?: "OnSessionEvent")
            }

            override fun onSessionDestroyed() {
                mediaBrowserConnectionCallback.onConnectionSuspended()
            }
        }

        mediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                    registerCallback(mediaControllerCallback)
                }
            }

            override fun onConnectionSuspended() {
            }

            override fun onConnectionFailed() {
            }
        }

    }

}