package com.kt.apps.media.mobile.ui.complex

import android.content.Context
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplex2Binding
import com.kt.apps.media.mobile.models.VideoDisplayState
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.TVChannelViewModel
import java.util.Stack
import javax.inject.Inject
import kotlin.math.abs

class ComplexActivity : BaseActivity<ActivityComplex2Binding>() {
    private val swipeThreshold = 100
    private val velocitySwipeThreshold = 100

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutRes: Int
        get() = R.layout.activity_complex2

    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(this, object: GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffY) > swipeThreshold && abs(velocityY) > velocitySwipeThreshold) {
                    if (diffY > 0) {
                        onSwipeBottom(e1, e2)
                    }
                }
                return false
            }
        })
    }

    private val motionLayout: MotionLayout by lazy {
        binding.complexMotionLayout
    }

    private val tvChannelViewModel: TVChannelViewModel? by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java].apply {
            this.tvWithLinkStreamLiveData.observe(this@ComplexActivity, linkStreamDataObserver)
        }
    }

    private val playbackViewModel: PlaybackViewModel by lazy {
        ViewModelProvider(this, factory)[PlaybackViewModel::class.java].apply {
            this.videoSizeStateLiveData.observe(this@ComplexActivity, videoSizeChangeObserver)
        }
    }
    private var cacheLoadedDisplayState: VideoDisplayState? = null
    private val currentDisplayState: VideoDisplayState?
        get() = playbackViewModel.videoSizeStateLiveData.value

    private val linkStreamDataObserver: Observer<DataState<TVChannelLinkStream>> by lazy {
        Observer {dataState ->
            when(dataState) {
                is DataState.Loading -> {
                }
                else -> {

                }
            }
        }
    }

    private val videoSizeChangeObserver: Observer<VideoDisplayState> by lazy {
        Observer {
            state ->
            when(state) {
                VideoDisplayState.FULLSCREEN -> changeToFullScreen()
                is VideoDisplayState.SUCCESS -> {
                    cacheLoadedDisplayState = state
                    calculateCurrentSize(state.videoSize)
                }
                is VideoDisplayState.LOADING -> {
                    motionLayout.transitionToState(R.id.end)
                }
                else -> {}
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        tvChannelViewModel
        playbackViewModel

    }

    override fun initAction(savedInstanceState: Bundle?) {

    }

    override fun onBackPressed() {
        if (currentDisplayState == VideoDisplayState.FULLSCREEN) {
            cacheLoadedDisplayState
                ?.run { this as? VideoDisplayState.SUCCESS }
                ?.let { playbackViewModel.collapseVideo(it.videoSize) }
            return
        }
        super.onBackPressed()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun calculateCurrentSize(size: VideoSize) {
        val wpx = this.resources.displayMetrics.widthPixels
        val hpx = this.resources.displayMetrics.heightPixels
        if (size.width == 0 || size.height == 0) {
            return
        }
        val newHeight = wpx  / (size.width * 1.0 / size.height)
        val percentage: Float = (newHeight * 1.0 / hpx).toFloat()

        motionLayout.getConstraintSet(R.id.end)?.let {
            it.setGuidelinePercent(R.id.guideline_complex, percentage)
            motionLayout.transitionToState(R.id.end)
        }
    }

    private fun changeToFullScreen() {
        motionLayout.transitionToState(R.id.fullscreen)
    }

    fun onSwipeBottom(e1: MotionEvent, e2: MotionEvent) {
        val hitRect = Rect()
        val location = intArrayOf(0, 0)
        binding.fragmentContainerPlayback.getHitRect(hitRect)
        binding.fragmentContainerPlayback.getLocationOnScreen(location)

        hitRect.offset(location[0], location[1])
        if (hitRect.contains(e1.x.toInt(), e1.y.toInt())) {
            Log.d(TAG, "onSwipeBottom: ")
            motionLayout.transitionToState(R.id.fullscreen)
        }
    }

}