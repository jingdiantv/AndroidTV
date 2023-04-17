package com.kt.apps.media.mobile.utils

import android.graphics.Rect
import android.view.View
import androidx.fragment.app.Fragment

val Fragment.screenWidth: Int
    get() = resources.displayMetrics.widthPixels

val Fragment.screenHeight: Int
    get() = resources.displayMetrics.heightPixels

val View.hitRectOnScreen: Rect
    get() {
        val hitRect = Rect()
        val location =  intArrayOf(0, 0)
        getHitRect(hitRect)
        getLocationOnScreen(location)
        hitRect.offset(location[0], location[1])
        return hitRect
    }