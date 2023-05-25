package com.kt.apps.media.mobile.utils

import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.kt.apps.core.utils.TAG
import kotlin.math.roundToInt


fun RecyclerView.fastSmoothScrollToPosition(newPosition: Int) {
    layoutManager?.startSmoothScroll((object : LinearSmoothScroller(context) {
        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START
        }
    }).apply {
        this.targetPosition = newPosition
    })
}