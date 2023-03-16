package com.kt.apps.core.utils

import android.graphics.Color
import androidx.core.graphics.*

fun Color.changeWithAlpha(alpha: Float): Int {
    return Color.pack(
        this.component1(),
        this.component2(),
        this.component3(),
        alpha
    ).toColorInt()
}