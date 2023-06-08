package com.kt.apps.media.mobile.ui.complex

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.video.VideoSize
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityComplexBinding
import com.kt.apps.media.mobile.ui.fragments.channels.IPlaybackAction
import com.kt.apps.media.mobile.ui.fragments.channels.PlaybackFragment
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import java.lang.ref.WeakReference
import javax.inject.Inject

enum class  PlaybackState {
    Fullscreen, Minimal, Invisible
}
class ComplexActivity : BaseActivity<ActivityComplexBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutRes: Int
        get() = R.layout.activity_complex

    private var layoutHandler: ComplexLayoutHandler? = null

    private val tvChannelViewModel: TVChannelViewModel? by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java].apply {
            this.tvWithLinkStreamLiveData.observe(this@ComplexActivity, linkStreamDataObserver)
        }
    }

    private val linkStreamDataObserver: Observer<DataState<TVChannelLinkStream>> by lazy {
        Observer {dataState ->
            when(dataState) {
                is DataState.Loading ->
                    layoutHandler?.onStartLoading()
                else -> return@Observer
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        tvChannelViewModel

        val metrics = resources.displayMetrics
        layoutHandler = if (metrics.widthPixels <= metrics.heightPixels) {
            PortraitLayoutHandler(WeakReference(this))
        } else {
            LandscapeLayoutHandler(WeakReference(this))
        }

        layoutHandler?.onPlaybackStateChange = {
            binding.fragmentContainerPlayback.getFragment<PlaybackFragment>().displayState = it
        }

    }

    override fun initAction(savedInstanceState: Bundle?) {
        binding.fragmentContainerPlayback.getFragment<PlaybackFragment>().apply {
            this.callback = object: IPlaybackAction {
                override fun onLoadedSuccess(videoSize: VideoSize) {
                    layoutHandler?.onLoadedVideoSuccess(videoSize)
                }

                override fun onOpenFullScreen() {
                    layoutHandler?.onOpenFullScreen()
                }

                override fun onPauseAction(userAction: Boolean) {
                    if (userAction) layoutHandler?.onPlayPause(isPause = true)
                }

                override fun onPlayAction(userAction: Boolean) {
                    if (userAction) layoutHandler?.onPlayPause(isPause = false)
                }
            }


        }

        //Deeplink handle
        handleIntent(intent)
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val deeplink = intent?.data ?: return

        if (deeplink.host?.equals(Constants.HOST_TV) == true || deeplink.host?.equals(Constants.HOST_RADIO) == true) {
            if(deeplink.path?.contains("channel") == true) {
                runOnUiThread {
                    tvChannelViewModel?.playMobileTvByDeepLinks(uri = deeplink)
                    intent.data = null
                }
            } else {
                intent.data = null
            }
        }
    }

}