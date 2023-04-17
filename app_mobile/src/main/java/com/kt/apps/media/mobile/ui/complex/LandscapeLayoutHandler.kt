package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplexBinding
import com.kt.apps.media.mobile.utils.hitRectOnScreen
import java.lang.ref.WeakReference

class LandscapeLayoutHandler(private val weakActivity: WeakReference<ComplexActivity>) : ComplexLayoutHandler  {
    sealed class State {
        object IDLE: State()
        object MINIMAL: State()
        object FULLSCREEN: State()
    }

    private var state: State = State.IDLE
    private var cachedVideoSize: VideoSize? = null

    private val context: Context?
        get() = weakActivity.get()

    private val fragmentContainerPlayback: View?
        get() = weakActivity.get()?.binding?.fragmentContainerPlayback

    override val motionLayout: MotionLayout?
        get() = weakActivity.get()?.binding?.complexMotionLayout

    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {

        }).apply {
            this.setOnDoubleTapListener(object: GestureDetector.OnDoubleTapListener {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Log.d(TAG, "onDoubleTap: ")
                    this@LandscapeLayoutHandler.onDoubleTap(e)
                    return true
                }

                override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                    return true
                }
            })
        }
    }
    override fun onStartLoading() {
        if (state != State.FULLSCREEN) {
            motionLayout?.setTransitionDuration(250)
            motionLayout?.transitionToState(R.id.fullscreen)
            state = State.FULLSCREEN
        }
    }

    override fun onLoadedVideoSuccess(videoSize: VideoSize) {
        cachedVideoSize = videoSize
        if (state != State.FULLSCREEN) {
            motionLayout?.setTransitionDuration(250)
            motionLayout?.transitionToState(R.id.fullscreen)
            state = State.FULLSCREEN
        }
    }

    override fun onOpenFullScreen() {
        state = if (state != State.FULLSCREEN) {
            motionLayout?.transitionToState(R.id.fullscreen)
            State.FULLSCREEN
        } else {
            motionLayout?.transitionToState(R.id.end)
            State.MINIMAL
        }
    }

    override fun onBackEvent(): Boolean {
        if (state == State.FULLSCREEN) {
            motionLayout?.transitionToState(R.id.end)
            state = State.MINIMAL
            return true
        }
        return false
    }

    override fun onReset(isPlaying: Boolean) {
        state = if (isPlaying) {
            motionLayout?.transitionToState(R.id.fullscreen)
            State.FULLSCREEN
        } else {
            motionLayout?.transitionToState(R.id.start)
            State.IDLE
        }
    }

    override fun onTouchEvent(ev: MotionEvent) {
        gestureDetector.onTouchEvent(ev)
    }

    private fun onDoubleTap(ev: MotionEvent) {
        val hitRect = Rect()
        fragmentContainerPlayback?.getHitRect(hitRect)
        if (hitRect.contains(ev.x.toInt(), ev.y.toInt())) {
            motionLayout?.transitionToState(R.id.fullscreen)
        }
    }
}