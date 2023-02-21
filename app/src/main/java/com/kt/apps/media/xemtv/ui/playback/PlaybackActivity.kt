package com.kt.apps.media.xemtv.ui.playback

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

/** Loads [PlaybackVideoFragment]. */
class PlaybackActivity : FragmentActivity(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val tvChannelViewModel: TVChannelViewModel by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        intent?.data?.let {
            tvChannelViewModel.playTvByDeepLinks(it)
        }
        tvChannelViewModel.getListTVChannel(true)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, PlaybackVideoFragment())
                .commit()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let {
            tvChannelViewModel.playTvByDeepLinks(it)
        }
    }
    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Logger.d(this, message = "onKeyDown: $keyCode")
        return super.onKeyDown(keyCode, event)
    }
}