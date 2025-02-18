package com.kt.apps.media.xemtv.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.autoupdate.ui.FragmentInfo
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.media.xemtv.BuildConfig
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.databinding.ActivityMainBinding
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.extensions.ExtensionsViewModel
import javax.inject.Inject

/**
 * Loads [DashboardFragment].
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var roomDataBase: RoomDataBase

    private val tvChannelViewModel by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
    }


    private val extensionsViewModel by lazy {
        ViewModelProvider(this, factory)[ExtensionsViewModel::class.java]
    }

    override val layoutRes: Int
        get() = R.layout.activity_main

    override fun initView(savedInstanceState: Bundle?) {
        screenWidth = binding.root.measuredWidth
    }

    override fun initAction(savedInstanceState: Bundle?) {
        FragmentInfo.appVersion = BuildConfig.VERSION_NAME
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_browse_fragment, DashboardFragment())
            .commitNow()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                ?.takeIf {
                    it is DashboardFragment
                }?.let {
                    (it as DashboardFragment).selectPageRowByUri(uri)
                }
        }
    }

    override fun onBackPressed() {
        supportFragmentManager.findFragmentById(android.R.id.content)
            ?.let {
                super.onBackPressed()
            }
            ?: supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                ?.takeIf {
                    it is DashboardFragment
                }?.let {
                    (it as DashboardFragment).apply {
                        this.onBackPressed()
                    }
                }
            ?: super.onBackPressed()

    }

    companion object {
        var screenWidth = -1
    }
}