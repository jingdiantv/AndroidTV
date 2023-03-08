package com.kt.apps.media.xemtv.ui.playback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.databinding.ActivityPlaybackBinding
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.football.FootballPlaybackFragment
import com.kt.apps.media.xemtv.ui.football.FootballViewModel
import com.kt.apps.media.xemtv.ui.main.MainActivity
import dagger.android.AndroidInjection
import dagger.android.HasAndroidInjector
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/** Loads [TVPlaybackVideoFragment]. */
class PlaybackActivity : BaseActivity<ActivityPlaybackBinding>(), HasAndroidInjector {
    override val layoutRes: Int
        get() = R.layout.activity_playback

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initAction(savedInstanceState: Bundle?) {
        intent?.data?.let {
            playContentByDeepLink(it, savedInstanceState)
        }
        when (intent.getParcelableExtra<Type>(EXTRA_PLAYBACK_TYPE)) {
            Type.FOOTBALL -> {
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, FootballPlaybackFragment())
                        .commit()
                }
            }

            Type.TV -> {
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, TVPlaybackVideoFragment())
                        .commit()
                }
            }

            else -> {}
        }

    }

    private fun playContentByDeepLink(deepLink: Uri, savedInstanceState: Bundle?) {
        when {
            deepLink.scheme.equals(Constants.SCHEME_FOOTBALL) -> {
                footballViewModel.streamFootballByDeepLinks(deepLink)
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, FootballPlaybackFragment())
                        .commit()
                }
            }

            deepLink.scheme.equals(Constants.SCHEME_TV) -> {
                tvChannelViewModel.playTvByDeepLinks(deepLink)
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, TVPlaybackVideoFragment())
                        .commit()
                }
            }

            else -> {
                startActivity(Intent(this, MainActivity::class.java)
                    .apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }
        }
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val tvChannelViewModel: TVChannelViewModel by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
    }
    private val footballViewModel: FootballViewModel by lazy {
        ViewModelProvider(this, factory)[FootballViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let {
            playContentByDeepLink(it, null)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Logger.d(this, message = "onKeyDown: $keyCode")
        val fragment: TVPlaybackVideoFragment = supportFragmentManager.findFragmentById(android.R.id.content)
            ?.takeIf {
                it is TVPlaybackVideoFragment
            }?.let {
                it as TVPlaybackVideoFragment
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
        val fragment: TVPlaybackVideoFragment = supportFragmentManager.findFragmentById(android.R.id.content)
            ?.takeIf {
                it is TVPlaybackVideoFragment
            }?.let {
                it as TVPlaybackVideoFragment
            } ?: return super.onBackPressed()
        if (fragment.canBackToMain()) {
            super.onBackPressed()
        } else {
            fragment.hideChannelMenu()
        }
    }

    @Parcelize
    enum class Type : Parcelable {
        TV, FOOTBALL
    }

    companion object {
        const val EXTRA_PLAYBACK_TYPE = "extra:playback_type"
        const val EXTRA_FOOTBALL_MATCH = "extra:football_match"
        fun start(activity: FragmentActivity, type: Type) {
            val intent = Intent(activity, PlaybackActivity::class.java)
            intent.putExtra(EXTRA_PLAYBACK_TYPE, type as Parcelable)
            activity.startActivity(intent)
        }
    }
}