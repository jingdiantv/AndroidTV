package com.kt.apps.core.base.activity

import android.content.Intent
import android.os.Bundle
import androidx.databinding.ViewDataBinding
import com.kt.apps.core.base.BaseActivity

abstract class BaseDeepLinkActivity<T : ViewDataBinding> : BaseActivity<T>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }
}