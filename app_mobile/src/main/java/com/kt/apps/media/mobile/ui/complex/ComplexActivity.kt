package com.kt.apps.media.mobile.ui.complex

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
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
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutRes: Int
        get() = R.layout.activity_complex2

    private lateinit var layoutHandler: ComplexLayoutHandler

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
                VideoDisplayState.FULLSCREEN -> layoutHandler.onOpenFullScreen()
                is VideoDisplayState.SUCCESS -> {
                    layoutHandler.onLoadedVideoSuccess(state.videoSize)
                }
                is VideoDisplayState.LOADING -> {
                    layoutHandler.onStartLoading()
                }
                else -> {}
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        tvChannelViewModel
        playbackViewModel

        layoutHandler = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            PortraitLayoutHandler(context = this, binding, motionLayout)
        } else {
            LandscapeLayoutHandler(context = this, binding, motionLayout)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        layoutHandler = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            PortraitLayoutHandler(context = this, binding, motionLayout)
        } else {
            LandscapeLayoutHandler(context = this, binding, motionLayout)
        }
        layoutHandler.onReset()
    }

    override fun initAction(savedInstanceState: Bundle?) {

    }

    override fun onBackPressed() {
        if (layoutHandler.onBackEvent()) {
            return
        }
        super.onBackPressed()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        layoutHandler.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

}