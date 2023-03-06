package com.kt.apps.media.xemtv.ui.playback

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.Action
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.utils.KeyCodeTranslator
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
        val fragment: PlaybackVideoFragment = supportFragmentManager.findFragmentById(android.R.id.content)
            ?.takeIf {
                it is PlaybackVideoFragment
            }?.let {
                it as PlaybackVideoFragment
            } ?: return super.onKeyDown(keyCode, event)

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                fragment.onDpadCenter()
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                fragment.onDpadDown()
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                fragment.onDpadUp()
            }

            KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> {
                fragment.onDpadLeft()
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                fragment.onDpadRight()
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {

            }

            KeyEvent.KEYCODE_VOLUME_UP -> {

            }

            KeyEvent.KEYCODE_BACK -> {
            }

            KeyEvent.KEYCODE_CHANNEL_UP -> {
                fragment.onKeyCodeChannelUp()
            }

            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                fragment.onKeyCodeChannelDown()
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                fragment.onKeyCodeChannelUp()
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                fragment.onKeyCodeChannelDown()
            }

        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        val fragment: PlaybackVideoFragment = supportFragmentManager.findFragmentById(android.R.id.content)
            ?.takeIf {
                it is PlaybackVideoFragment
            }?.let {
                it as PlaybackVideoFragment
            } ?: return super.onBackPressed()
        if (fragment.canBackToMain()) {
            super.onBackPressed()
        } else {
            fragment.hideChannelMenu()
        }
    }
}