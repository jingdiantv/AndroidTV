package com.kt.apps.core.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

abstract class BaseBottomSheetFragment<T : ViewDataBinding> : BottomSheetDialogFragment(),
    HasAndroidInjector {
    protected lateinit var binding: T
    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    abstract val resLayout: Int
    abstract fun initView(savedInstanceState: Bundle?)
    abstract fun initAction(savedInstanceState: Bundle?)


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, resLayout, container, false)
        initView(savedInstanceState)
        initAction(savedInstanceState)
        return binding.root
    }

}