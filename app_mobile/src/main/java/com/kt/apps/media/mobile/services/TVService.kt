package com.kt.apps.media.mobile.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.util.NotificationUtil
import com.kt.apps.media.mobile.ui.playback.ITVServiceAidlInterface


class TVService : Service() {
    private val binder = object : ITVServiceAidlInterface.Stub() {
        override fun sendData(aString: String?) {

        }

        override fun getChannelListJson(): String {
            return ""
        }

        override fun getChannelJson(channelId: String?): String {
            return ""
        }

        override fun writeJsonData(jsonData: String?) {
        }

    }

    override fun onCreate() {
        super.onCreate()
        Log.e("TAG", "Auto create")
        NotificationUtil.createNotificationChannel(
            this,
            NOTIFICATION_CHANNEL,
            com.kt.apps.core.R.string.app_name,
            com.kt.apps.core.R.string.app_name,
            NotificationUtil.IMPORTANCE_HIGH
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .build()

        startForeground(123, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.e("TAG", "onBind")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("TAG", "onDestroy")
    }

    companion object {
        private const val NOTIFICATION_CHANNEL = "iMedia"
    }
}