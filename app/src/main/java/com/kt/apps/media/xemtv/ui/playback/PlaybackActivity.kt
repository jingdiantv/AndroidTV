package com.kt.apps.media.xemtv.ui.playback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.football.model.FootballMatchWithStreamLink
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
        when (intent.getParcelableExtra<Type>(EXTRA_PLAYBACK_TYPE)) {
            Type.FOOTBALL -> {
                footballViewModel.getAllMatches()
                supportFragmentManager.beginTransaction()
                    .replace(
                        android.R.id.content, FootballPlaybackFragment.newInstance(
                            intent.extras!!.getParcelable(EXTRA_FOOTBALL_MATCH)!!
                        )
                    )
                    .commit()
            }

            Type.TV, Type.RADIO -> {
                if (savedInstanceState == null) {
                    tvChannelViewModel.getListTVChannel(false)
                    supportFragmentManager.beginTransaction()
                        .replace(
                            android.R.id.content, TVPlaybackVideoFragment.newInstance(
                                intent.getParcelableExtra(EXTRA_PLAYBACK_TYPE)!!,
                                intent.extras!!.getParcelable(EXTRA_TV_CHANNEL)!!

                            )
                        )
                        .commit()
                }
            }

            else -> {
                intent?.data?.let {
                    playContentByDeepLink(it, savedInstanceState)
                }
            }
        }

        footballViewModel.footMatchDataState.observe(this) { dataState ->
            when (dataState) {
                is DataState.Error -> {
                    Logger.e(this, exception = dataState.throwable)
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent().apply {
                            data = Uri.parse("xemtv://bongda/dashboard")
                        })
                    }, 5000)

                    Handler(Looper.getMainLooper()).postDelayed({
                        showErrorDialog(
                            content = dataState.throwable.message,
                            autoDismiss = false
                        )
                    }, 2000)

                }

                else -> {

                }
            }
        }

    }

    private fun playContentByDeepLink(deepLink: Uri, savedInstanceState: Bundle?) {
        when {
            deepLink.host.equals(Constants.HOST_FOOTBALL) -> {
                footballViewModel.streamFootballByDeepLinks(deepLink)
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, FootballPlaybackFragment())
                        .commit()
                }
                intent?.data = null
            }

            deepLink.host.equals(Constants.HOST_TV) || deepLink.host.equals(Constants.HOST_RADIO) -> {
                tvChannelViewModel.playTvByDeepLinks(deepLink)
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, TVPlaybackVideoFragment())
                        .commit()
                }
                intent?.data = null
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Logger.d(this, message = "OnNewIntent: ${intent?.getParcelableExtra<Type>(EXTRA_PLAYBACK_TYPE)}")
        Logger.d(
            this, message = "OnNewIntent: ${
                intent?.extras?.getParcelable<FootballMatchWithStreamLink>(
                    EXTRA_FOOTBALL_MATCH
                )
            }"
        )
        Logger.d(this, message = "OnNewIntent: ${intent?.data}")
        intent?.data?.let {
            playContentByDeepLink(it, null)
        }
        when (intent?.getParcelableExtra<Type>(EXTRA_PLAYBACK_TYPE)) {
            Type.FOOTBALL -> {
                Logger.d(this, message = "Football")
                footballViewModel.getAllMatches()
                supportFragmentManager.beginTransaction()
                    .replace(
                        android.R.id.content, FootballPlaybackFragment.newInstance(
                            intent.extras!!.getParcelable(EXTRA_FOOTBALL_MATCH)!!
                        )
                    )
                    .commit()
            }

            Type.TV, Type.RADIO -> {
                tvChannelViewModel.getListTVChannel(false)
                supportFragmentManager.beginTransaction()
                    .replace(
                        android.R.id.content, TVPlaybackVideoFragment.newInstance(
                            intent.getParcelableExtra(EXTRA_PLAYBACK_TYPE)!!,
                            intent.extras!!.getParcelable(EXTRA_TV_CHANNEL)!!
                        )
                    )
                    .commit()

            }

            else -> {

            }
        }

    }

    override fun onPause() {
        super.onPause()
        tvChannelViewModel.clearCurrentPlayingChannelState()
        footballViewModel.clearState()
    }

    @Parcelize
    enum class Type : Parcelable {
        TV, FOOTBALL, RADIO
    }

    companion object {
        const val EXTRA_PLAYBACK_TYPE = "extra:playback_type"
        const val EXTRA_FOOTBALL_MATCH = "extra:football_match"
        const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        fun start(activity: FragmentActivity, type: Type) {
            val intent = Intent(activity, PlaybackActivity::class.java)
            intent.putExtra(EXTRA_PLAYBACK_TYPE, type as Parcelable)
            activity.startActivity(intent)
        }
    }
}