package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import androidx.constraintlayout.motion.widget.MotionLayout
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplex2Binding

sealed class LandscapeLayoutState {
    object IDLE: LandscapeLayoutState()
    object MINIMAL: LandscapeLayoutState()
    object FULLSCREEN: LandscapeLayoutState()
}

class LandscapeLayoutHandler(context: Context, val binding: ActivityComplex2Binding, override val motionLayout: MotionLayout?) : ComplexLayoutHandler  {
    private var state: LandscapeLayoutState = LandscapeLayoutState.IDLE
    private var cachedVideoSize: VideoSize? = null

    override fun onStartLoading() {
        if (state != LandscapeLayoutState.FULLSCREEN) {
            motionLayout?.setTransitionDuration(250)
            motionLayout?.transitionToState(R.id.fullscreen)
            state = LandscapeLayoutState.FULLSCREEN
        }
    }

    override fun onLoadedVideoSuccess(videoSize: VideoSize) {
        cachedVideoSize = videoSize
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

    override fun onReset() {
        motionLayout?.transitionToState(R.id.fullscreen)
        state = LandscapeLayoutState.FULLSCREEN
    }
}