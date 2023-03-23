package com.kt.apps.media.mobile.ui.main

import androidx.work.WorkManager
import com.kt.apps.core.tv.viewmodels.BaseTVChannelViewModel
import com.kt.apps.core.tv.viewmodels.TVChannelInteractors
import javax.inject.Inject

class TVChannelViewModel @Inject constructor(
    interactors: TVChannelInteractors,
    private val workManager: WorkManager
) : BaseTVChannelViewModel(interactors) {

    companion object {
        private var instance = 0
    }
}