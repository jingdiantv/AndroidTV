package com.kt.apps.media.xemtv.ui

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.viewmodels.BaseTVChannelViewModel
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import com.kt.apps.media.xemtv.App
import com.kt.apps.media.xemtv.workers.TVRecommendationWorkers
import javax.inject.Inject

class TVChannelViewModel @Inject constructor(
    interactors: TVChannelInteractors,
    private val workManager: WorkManager
) : BaseTVChannelViewModel(interactors) {
    override fun enqueueInsertWatchNextTVChannel(tvChannel: TVChannel) {
        workManager.enqueue(
            OneTimeWorkRequestBuilder<TVRecommendationWorkers>()
                .setInputData(
                    Data.Builder()
                        .putInt(TVRecommendationWorkers.EXTRA_TYPE, TVRecommendationWorkers.Type.WATCH_NEXT.value)
                        .putString(TVRecommendationWorkers.EXTRA_TV_PROGRAM_ID, tvChannel.channelId)
                        .build()
                )
                .build()
        )
    }

    override fun onFetchTVListSuccess(listChannel: List<TVChannel>) {
        super.onFetchTVListSuccess(listChannel)
        workManager.enqueue(
            OneTimeWorkRequestBuilder<TVRecommendationWorkers>()
                .setInputData(
                    Data.Builder()
                        .putInt(TVRecommendationWorkers.EXTRA_TYPE, TVRecommendationWorkers.Type.ALL.value)
                        .build())
                .build()
        )
    }

    init {
        instance++
        Logger.d(this, message = "TVChannelViewModel instance count: $instance")
    }

    companion object {
        private var instance = 0
    }
}