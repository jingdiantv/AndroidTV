package com.kt.apps.autoupdate.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.autoupdate.AppUpdateManager
import com.kt.apps.autoupdate.R
import com.kt.apps.autoupdate.databinding.ActivityAppUpdateBinding
import com.kt.apps.autoupdate.di.AppUpdateComponent
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.DataState
import javax.inject.Inject

class AppUpdateActivity : BaseActivity<ActivityAppUpdateBinding>() {

    override val layoutRes: Int
        get() = R.layout.activity_app_update

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, factory)[AppUpdateViewModel::class.java]
    }

    override fun initView(savedInstanceState: Bundle?) {
        setBackgroundOverlay()
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_content, FragmentInfo())
            .commit()
    }

    override fun initAction(savedInstanceState: Bundle?) {
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

}