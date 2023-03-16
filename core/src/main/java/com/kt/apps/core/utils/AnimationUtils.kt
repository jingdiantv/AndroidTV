package com.kt.apps.core.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.kt.apps.core.R
import kotlin.math.hypot

object AnimationUtils {
    @JvmStatic
    fun getRotateAnimation(isArrowDown: Boolean, width: Int, height: Int): RotateAnimation =
        if (isArrowDown) {
            RotateAnimation(
                0f,
                180f,
                width / 2.toFloat(),
                height / 2.toFloat()
            )
        } else {
            RotateAnimation(
                180f,
                360f,
                width / 2.toFloat(),
                height / 2.toFloat()
            )
        }

}

fun RecyclerView.runLayoutAnimation() {
    val layoutAnimation =
        AnimationUtils.loadLayoutAnimation(context, com.kt.skeleton.R.anim.recycler_view_layout_anim_fall_down)
    setLayoutAnimation(layoutAnimation)
    adapter?.notifyDataSetChanged()
    scheduleLayoutAnimation()
}

fun View.startHideOrShowAnimation(
    shouldShow: Boolean,
    onAnimationEnd: () -> Unit
) {
    val repeatAnimView = when {
        shouldShow && visibility == View.VISIBLE -> true
        !shouldShow && visibility == View.INVISIBLE -> true
        else -> false
    }
    if (repeatAnimView) return

    val cx: Int?
    val cy: Int?
    val initRadius: Float?
    cx = width / 2
    cy = height / 2
    initRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
    val anim: Animator
    val startRadius: Float
    val endRadius: Float

    if (shouldShow) {
        startRadius = 0f
        endRadius = initRadius
    } else {
        startRadius = initRadius
        endRadius = 0f
    }

    anim = ViewAnimationUtils.createCircularReveal(
        this,
        cx,
        cy,
        startRadius,
        endRadius
    )
    anim.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            super.onAnimationEnd(animation)
            gone()
            anim.removeAllListeners()
        }
    })
    anim.start()

}

fun View.setVisible(shouldVisible: Boolean) {
    this.visibility = if (shouldVisible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

fun View.inVisible() {
    this.visibility = View.INVISIBLE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun SwipeRefreshLayout.setColorSchemaDefault(context: Context) {
    setColorSchemeColors(
        Color.DKGRAY,
        ContextCompat.getColor(context, R.color.startGradientIconColorHighlight),
        Color.GREEN,
        ContextCompat.getColor(context, R.color.backgroundColor),
    )
}

fun CircularProgressDrawable.setColorSchemaDefault(context: Context) {
    setColorSchemeColors(
        Color.DKGRAY,
        ContextCompat.getColor(context, R.color.startGradientIconColorHighlight),
        Color.GREEN,
        ContextCompat.getColor(context, R.color.backgroundColor),
    )
}
