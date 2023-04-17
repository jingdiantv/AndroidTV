package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplexBinding
import java.lang.ref.WeakReference

sealed class LandscapeLayoutState {
    object IDLE: LandscapeLayoutState()
    object MINIMAL: LandscapeLayoutState()
    object FULLSCREEN: LandscapeLayoutState()
}

class LandscapeLayoutHandler(val weakActivity: WeakReference<ComplexActivity>) : ComplexLayoutHandler  {
    private var state: LandscapeLayoutState = LandscapeLayoutState.IDLE
    private var cachedVideoSize: VideoSize? = null

    private val context: Context?
        get() = weakActivity.get()

    private val fragmentContainerPlayback: View?
        get() = weakActivity.get()?.binding?.fragmentContainerPlayback

    override val motionLayout: MotionLayout?
        get() = weakActivity.get()?.binding?.complexMotionLayout
    override fun onStartLoading() {
        if (state != LandscapeLayoutState.FULLSCREEN) {
            motionLayout?.setTransitionDuration(250)
            motionLayout?.transitionToState(R.id.fullscreen)
            state = LandscapeLayoutState.FULLSCREEN
        }
    }

    override fun onLoadedVideoSuccess(videoSize: VideoSize) {
        cachedVideoSize = videoSize
        if (state != LandscapeLayoutState.FULLSCREEN) {
            motionLayout?.setTransitionDuration(250)
            motionLayout?.transitionToState(R.id.fullscreen)
            state = LandscapeLayoutState.FULLSCREEN
        }
    }

    override fun onOpenFullScreen() {
        state = if (state != LandscapeLayoutState.FULLSCREEN) {
            motionLayout?.transitionToState(R.id.fullscreen)
            LandscapeLayoutState.FULLSCREEN
        } else {
            motionLayout?.transitionToState(R.id.end)
            LandscapeLayoutState.MINIMAL
        }
    }

    override fun onBackEvent(): Boolean {
        if (state == LandscapeLayoutState.FULLSCREEN) {
            motionLayout?.transitionToState(R.id.end)
            state = LandscapeLayoutState.MINIMAL
            return true
        }
        return false
    }

    override fun onReset(isPlaying: Boolean) {
        state = if (isPlaying) {
            motionLayout?.transitionToState(R.id.fullscreen)
            LandscapeLayoutState.FULLSCREEN
        } else {
            motionLayout?.transitionToState(R.id.start)
            LandscapeLayoutState.IDLE
        }
    }
}