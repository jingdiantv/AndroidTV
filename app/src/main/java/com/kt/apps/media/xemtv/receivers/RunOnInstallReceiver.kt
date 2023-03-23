package com.kt.apps.media.xemtv.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.media.xemtv.workers.TVRecommendationWorkers

class RunOnInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.d(this, message = intent.action ?: "")
        when (intent.action) {
            TvContractCompat.ACTION_INITIALIZE_PROGRAMS -> {

            }
            TvContractCompat.ACTION_PREVIEW_PROGRAM_ADDED_TO_WATCH_NEXT,
            TvContractCompat.ACTION_WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED,
            TvContractCompat.ACTION_PREVIEW_PROGRAM_BROWSABLE_DISABLED-> {
                val programId =
                    intent.extras?.getLong(TvContractCompat.EXTRA_PREVIEW_PROGRAM_ID)
                Logger.d(this, message = "User added program $programId to watch next")

            }

            else -> {

            }
        }
    }

}