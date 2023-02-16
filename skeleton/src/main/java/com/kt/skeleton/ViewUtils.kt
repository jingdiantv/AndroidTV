package com.kt.skeleton

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.annotation.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.forEach
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel

enum class FontType(val value: Int) {
    REGULAR(0), MEDIUM(1), SEMI_BOLD(2)
}


fun TextView.bindFontText(type: Int, isBold: Boolean) {
    var tf: Typeface? = null
    var style = Typeface.NORMAL
    when (type) {
        FontType.REGULAR.value -> {
            tf = Typeface.createFromAsset(
                context.assets,
                "fonts/roboto_regular.ttf"
            )
        }

        FontType.MEDIUM.value -> {
            tf = Typeface.createFromAsset(
                context.assets,
                "fonts/roboto_medium.ttf"
            )
        }

        FontType.SEMI_BOLD.value -> {
            tf = Typeface.createFromAsset(
                context.assets,
                "fonts/roboto_medium.ttf"
            )
            style = Typeface.BOLD
        }
        else -> {

        }
    }
    if (isBold) {
        style = Typeface.BOLD
    }
    if (tf != null) {
        setTypeface(tf, style)
    }
}

fun View.runAnimationChangeBackground(fromColor: Int, toColor: Int, duration: Int) {
    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
    colorAnimation.duration = duration.toLong()
    colorAnimation.addUpdateListener { animator: ValueAnimator -> this.setBackgroundColor(animator.animatedValue as Int) }
    colorAnimation.start()
}

fun View.runAnimationChangeBackground(fromColor: Int, toColor: Int) {
    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
    colorAnimation.duration = 700
    colorAnimation.addUpdateListener { animator: ValueAnimator -> this.setBackgroundColor(animator.animatedValue as Int) }
    colorAnimation.start()
}

@SuppressLint("WrongConstant")
fun View.runAnimationChangeBackground() {
    val animationDrawable = background as AnimationDrawable
    animationDrawable.setEnterFadeDuration(700)
    animationDrawable.setExitFadeDuration(500)
    animationDrawable.isOneShot = false
    animationDrawable.start()
}

fun View.bindAnimationDrawable() {
    if (background is AnimationDrawable) return
    when (this.background.constantState) {
        ResourcesCompat.getDrawable(
            resources,
            R.drawable.background_circle,
            null
        )?.constantState,
        -> {
            bindAnimationDrawable(BackgroundType.CIRCLE)
        }
        ResourcesCompat.getDrawable(
            resources,
            R.drawable.background_corners_4dp,
            null
        )?.constantState,
        ResourcesCompat.getDrawable(
            resources,
            R.drawable.background_corners_8dp,
            null
        )?.constantState,
        -> {
            bindAnimationDrawable(BackgroundType.TEXTVIEW)
        }
        else -> {
            bindAnimationDrawable(BackgroundType.NO_ROUNDER)
        }
    }
}

fun View.bindAnimationDrawable(type: BackgroundType) {
    this.background = resources.getDrawable(type.value)
}

fun View.runFadeInAnimation(duration: Long) {
    val anim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.fade_in_rv)
    anim.duration = duration
    anim.repeatCount = ObjectAnimator.INFINITE
    anim.repeatMode = ObjectAnimator.REVERSE
    anim.fillAfter = true
    anim.fillBefore = true
    startAnimation(anim)
}

fun View.runFadeInAnimation() {
    runFadeInAnimation(900)
}

fun View.runAnimationChangeBackgroundColor(fromColor: Int, toColor: Int, duration: Int) {
    val animation =
        ObjectAnimator.ofObject(this, "backgroundColor", ArgbEvaluator(), fromColor, toColor)
            .setDuration(duration.toLong())
    animation.repeatCount = Animation.INFINITE
    animation.repeatMode = ValueAnimator.REVERSE
    animation.start()
}

fun View.runTransitionDrawable(duration: Int) {
    val background: TransitionDrawable = background as TransitionDrawable
    background.startTransition(duration)
}


fun View.setMarginBottomToParent(margin: Int) {
    var params: ViewGroup.LayoutParams? = null
    when (parent) {
        is CoordinatorLayout -> {
            params = layoutParams as CoordinatorLayout.LayoutParams
            params.bottomMargin = margin
        }
        is ConstraintLayout -> {
            params = layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = margin
        }
    }
    if (params != null) {
        this.layoutParams = params
    }
}


fun ViewGroup.replaceView(currentView: View, targetView: View) {
    val sourceViewIndexInParent = indexOfChild(currentView)
    val childLayoutParams = currentView.layoutParams
    removeView(currentView)
    targetView.id = currentView.id
    if (targetView is ViewGroup) {
        targetView.removeAllViews()
    }
    addView(targetView, sourceViewIndexInParent, childLayoutParams)
}


fun ViewGroup.runChildAnimDrawable() {
    forEach {
        while (it is ViewGroup) {
            runChildAnimDrawable()
        }
        it.bindAnimationDrawable()
        it.runAnimationChangeBackground()
    }
}

fun ViewGroup.replaceView(
    currentView: View,
    targetView: View,
    layoutParams: ViewGroup.LayoutParams,
) {
    val sourceViewIndexInParent = indexOfChild(currentView)
    removeView(currentView)
    targetView.id = currentView.id
    addView(targetView, sourceViewIndexInParent, layoutParams)
}

fun View.makeVisible() {
    visibility = View.VISIBLE
}

fun View.makeInVisible() {
    visibility = View.INVISIBLE
}

fun View.makeGone() {
    visibility = View.GONE
}

fun Int.dpToPx(context: Context): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
        context.resources.displayMetrics
    ).toInt()

internal fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return context.layoutInflater.inflate(layoutRes, this, attachToRoot)
}

internal val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

internal val Context.inputMethodManager
    get() = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

internal inline fun Boolean?.orFalse(): Boolean = this ?: false

internal fun Context.getDrawableCompat(@DrawableRes drawable: Int) =
    ContextCompat.getDrawable(this, drawable)

internal fun Context.getColorCompat(@ColorRes color: Int) = ContextCompat.getColor(this, color)

fun TextView.setTextColorRes(@ColorRes color: Int) = setTextColor(context.getColorCompat(color))

fun GradientDrawable.setCornerRadius(
    topLeft: Float = 0F,
    topRight: Float = 0F,
    bottomRight: Float = 0F,
    bottomLeft: Float = 0F,
) {
    cornerRadii = arrayOf(
        topLeft, topLeft,
        topRight, topRight,
        bottomRight, bottomRight,
        bottomLeft, bottomLeft
    ).toFloatArray()
}

fun convertDpToPixel(dp: Float, context: Context): Float {
    return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun convertPixelsToDp(px: Float, context: Context): Float {
    return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun AutoCompleteTextView.setOnAddTagListener(callback: (name: String) -> Unit) {
    // select from adapter list
    setOnItemClickListener { adapterView, _, position, _ ->
        val name = adapterView.getItemAtPosition(position) as String
        callback(name)
    }

    // done pressed
    setOnEditorActionListener { textView, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val name = textView.text.toString()

            callback(name)

            true
        } else false
    }

    doAfterTextChanged { text ->
        if (text != null && text.isEmpty()) {
            return@doAfterTextChanged
        }

        if (text?.last() == ',') { //  || text?.last() == ' '
            val name = text.substring(0, text.length - 1)
            callback(name)
        }
    }
}

fun TextSwitcher.setAnimation(@AnimRes inAnim: Int, @AnimRes outAnim: Int) {
    val `in`: Animation = AnimationUtils.loadAnimation(this.context, inAnim)
    val out: Animation = AnimationUtils.loadAnimation(this.context, outAnim)
    inAnimation = `in`
    outAnimation = out
}

fun ShapeableImageView.setAllCornerRadius(cornerRadius: Int = 10) {
    shapeAppearanceModel = ShapeAppearanceModel.Builder()
        .setAllCornerSizes(cornerRadius * Resources.getSystem().displayMetrics.scaledDensity)
        .build()
}

fun RecyclerView.runLayoutAnimation() {
    val layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.recycler_view_layout_anim_fall_down)
    setLayoutAnimation(layoutAnimation)
    adapter?.notifyDataSetChanged()
    scheduleLayoutAnimation()
}

