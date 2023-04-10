package com.kt.apps.media.xemtv.ui

import android.os.Bundle
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.databinding.ActivityDialogBinding
import com.kt.apps.media.xemtv.ui.extensions.FragmentAddExtensions

class DialogActivity : BaseActivity<ActivityDialogBinding>() {
    override val layoutRes: Int
        get() = R.layout.activity_dialog

    override fun initView(savedInstanceState: Bundle?) {
        when(intent.getIntExtra(EXTRA_RES_LAYOUT_FRAGMENT, R.layout.fragment_add_extensions)) {
            R.layout.fragment_add_extensions -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.root, FragmentAddExtensions())
                    .commitNow()
            }
        }
    }

    override fun initAction(savedInstanceState: Bundle?) {
    }

    companion object {
        const val EXTRA_RES_LAYOUT_FRAGMENT = "extra:res_layout_fragment"
    }

}