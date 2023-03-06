package com.kt.apps.core.base

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.leanback.app.PlaybackSupportFragment
import com.kt.apps.core.R
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

abstract class BasePlaybackFragment : PlaybackSupportFragment(), HasAndroidInjector {

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    var mVideoSurface: SurfaceView? = null
    var mMediaPlaybackCallback: SurfaceHolder.Callback? = null

    var mState = SURFACE_NOT_CREATED


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        mVideoSurface = LayoutInflater.from(context)
            .inflate(R.layout.core_layout_surfaces, root, false) as SurfaceView

        root.addView(mVideoSurface, 0)
        mVideoSurface!!.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mMediaPlaybackCallback?.surfaceCreated(holder)
                mState = SURFACE_CREATED
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                mMediaPlaybackCallback?.surfaceChanged(holder, format, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mMediaPlaybackCallback?.surfaceDestroyed(holder)
                mState = SURFACE_NOT_CREATED
            }
        })
        backgroundType = BG_LIGHT
        return root
    }


    override fun androidInjector(): AndroidInjector<Any> {
        return injector
    }

    /**
     * Adds [SurfaceHolder.Callback] to [android.view.SurfaceView].
     */
    fun setSurfaceHolderCallback(callback: SurfaceHolder.Callback?) {
        mMediaPlaybackCallback = callback
        if (callback != null) {
            if (mState == SURFACE_CREATED) {
                mMediaPlaybackCallback!!.surfaceCreated(mVideoSurface!!.holder)
            }
        }
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        val screenWidth = requireView().width
        val screenHeight = requireView().height
        val p = mVideoSurface!!.layoutParams
        if (screenWidth * height > width * screenHeight) {
            p.height = screenHeight
            p.width = screenHeight * width / height
        } else {
            p.width = screenWidth
            p.height = screenWidth * height / width
        }
        mVideoSurface!!.layoutParams = p
    }

    /**
     * Returns the surface view.
     */
    fun getSurfaceView(): SurfaceView? {
        return mVideoSurface
    }

    override fun onDestroyView() {
        mVideoSurface = null
        mState = SURFACE_NOT_CREATED
        super.onDestroyView()
    }

    companion object {
        private const val SURFACE_NOT_CREATED = 0
        private const val SURFACE_CREATED = 1
    }
}