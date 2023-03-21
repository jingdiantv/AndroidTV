package com.kt.apps.media.xemtv.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kt.apps.media.xemtv.workers.TVRecommendationWorkers

class RunOnInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TvContractCompat.ACTION_INITIALIZE_PROGRAMS -> {

                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<TVRecommendationWorkers>().build()
                )

            }
            else -> {

            }
        }
    }

}