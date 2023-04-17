package com.kt.apps.media.mobile.ui.fragments.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kt.apps.core.base.BaseDialogFragment
import com.kt.apps.core.utils.TAG
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.AddExtensionDialogBinding
import com.kt.apps.media.mobile.utils.debounce
import com.kt.apps.media.mobile.utils.textChanges
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddExtensionFragment: BaseDialogFragment<AddExtensionDialogBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory

    override val layoutResId: Int
        get() = R.layout.add_extension_dialog

    private val sourceNameEditText by lazy {
        binding.extensionSourceName
    }

    private val sourceLinkEditText by lazy {
        binding.extensionSourceLink
    }

    private val saveButton by lazy {
        binding.saveButton
    }

    private val debounceOnClickListener by lazy {
        debounce<Unit>(250L, viewLifecycleOwner.lifecycleScope) {

        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        Log.d(TAG, "initView: ")
    }

    override fun initAction(savedInstanceState: Bundle?) {
        saveButton.setOnClickListener {
            debounceOnClickListener(Unit)
        }
        saveButton.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            combine(flow = sourceNameEditText.textChanges(), flow2 = sourceLinkEditText.textChanges(), transform = {
                    name, link -> (name.toString() ?: "") + (link.toString() ?: "")
            }).collect {
                saveButton.isEnabled = it.isNotEmpty()
            }
        }

    }

    override fun onResume() {
        dialog?.window?.let {  window ->
            window.windowManager?.defaultDisplay?.let {display ->
                val size = Point()
                display.getSize(size)
                window.setLayout((size.x * 0.8).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
                window.setGravity(Gravity.CENTER)
            }
        }
        super.onResume()
    }

    companion object {
        val TAG: String
            get() = AddExtensionFragment::class.java.simpleName
    }

}

