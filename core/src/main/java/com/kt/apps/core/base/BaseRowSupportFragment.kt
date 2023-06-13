package com.kt.apps.core.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.app.ProgressBarManager
import com.kt.apps.core.base.leanback.RowsSupportFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

abstract class BaseRowSupportFragment : RowsSupportFragment(), HasAndroidInjector {

    val progressManager: ProgressBarManager by lazy {
        ProgressBarManager()
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        initView(root!!)
        progressManager.initialDelay = 500
        progressManager.setRootView(requireActivity().findViewById(android.R.id.content))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAction(view)
    }

    override fun onDestroyView() {
        try {
            progressManager.hide()
        } catch (_: Exception) {
        }
        super.onDestroyView()
    }

    abstract fun initView(rootView: View)
    abstract fun initAction(rootView: View)
}