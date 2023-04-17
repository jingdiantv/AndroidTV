package com.kt.apps.media.mobile.ui.complex

import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import android.view.OrientationEventListener
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplexBinding
import com.kt.apps.media.mobile.ui.fragments.channels.IPlaybackAction
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackViewModel
import com.kt.apps.media.mobile.ui.main.TVChannelViewModel
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class ComplexActivity : BaseActivity<ActivityComplexBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutRes: Int
        get() = R.layout.activity_complex

    private var isPlaying: Boolean = false

    private val _portraitLayoutHandler: PortraitLayoutHandler by lazy {
        PortraitLayoutHandler(WeakReference(this))
    }

    private val _landscapeLayoutHandler: LandscapeLayoutHandler by lazy {
        LandscapeLayoutHandler(WeakReference(this))
    }
    private var layoutHandler: ComplexLayoutHandler? = null

    private val tvChannelViewModel: TVChannelViewModel? by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java].apply {
            this.tvWithLinkStreamLiveData.observe(this@ComplexActivity, linkStreamDataObserver)
        }
    }

    private val linkStreamDataObserver: Observer<DataState<TVChannelLinkStream>> by lazy {
        Observer {dataState ->
            when(dataState) {
                is DataState.Loading -> {
                    layoutHandler?.onStartLoading()
                }
                is DataState.Success -> {
                    isPlaying  = true
                }
                else -> {

                }
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        tvChannelViewModel

        val metrics = resources.displayMetrics
        layoutHandler = if (metrics.widthPixels <= metrics.heightPixels) {
            _portraitLayoutHandler
        } else {
           _landscapeLayoutHandler
        }

        binding.fragmentContainerPlayback.getFragment<PlaybackFragment>().apply {
            this.callback = object: IPlaybackAction {
                override fun onLoadedSuccess(videoSize: VideoSize) {
                    layoutHandler?.onLoadedVideoSuccess(videoSize)
                }

                override fun onOpenFullScreen() {
                    layoutHandler?.onOpenFullScreen()
                }
            }
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {

    }


    override fun onBackPressed() {
        if (layoutHandler?.onBackEvent() == true) {
            return
        }
        super.onBackPressed()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        layoutHandler?.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

}