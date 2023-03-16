package com.kt.apps.media.xemtv.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.databinding.ActivityMainBinding
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import javax.inject.Inject

/**
 * Loads [DashboardFragment].
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val tvChannelViewModel by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
    }

    override val layoutRes: Int
        get() = R.layout.activity_main

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initAction(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, DashboardFragment())
                .commitNow()
        }
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
}