package com.kt.apps.media.xemtv.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class FragmentAddExtensions : BaseRowSupportFragment() {

    @Inject
    lateinit var roomDataBase: RoomDataBase
    private val disposable by lazy {
        CompositeDisposable()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context)
            .inflate(R.layout.fragment_add_extensions, container, false)
    }

    override fun initView(rootView: View) {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Logger.e(this, message = "onViewCreated")
        if (mainFragmentAdapter != null) {
            mainFragmentAdapter.fragmentHost.notifyViewCreated(mainFragmentAdapter)
        }
    }

    override fun initAction(rootView: View) {
        view?.findViewById<View>(R.id.btn_save)?.setOnFocusChangeListener { v, hasFocus ->
            Logger.d(this@FragmentAddExtensions, message = "on focus")
        }
        rootView.findViewById<View>(R.id.btn_save)!!.setOnKeyListener { v, keyCode, event ->
            Logger.d(this, message = "$keyCode")
            return@setOnKeyListener true
        }

        rootView.findViewById<View>(R.id.btn_save)!!.setOnClickListener {
            Logger.d(this@FragmentAddExtensions, message = "On Click")
            if (!view?.findViewById<TextInputEditText>(R.id.textInputEditText_2)?.text.toString().startsWith("http")) {
                showErrorDialog(content = "Đường dẫn không hợp lệ! Vui lòng thử lại")
                return@setOnClickListener
            }
            disposable.add(
                roomDataBase.extensionsConfig()
                    .insert(
                        ExtensionsConfig(
                            view?.findViewById<TextInputEditText>(R.id.textInputEditText)?.text.toString(),
                            view?.findViewById<TextInputEditText>(R.id.textInputEditText_2)?.text.toString(),
                        )
                    ).subscribe({
                        Logger.d(this@FragmentAddExtensions, message = "Save link success")
                    }, {
                        Logger.e(this@FragmentAddExtensions, exception = it)
                    })
            )
        }
    }

    fun onDpadUp() {
        Logger.e(this, message = "onDpadUp")
        if (view?.findViewById<View>(R.id.textInputEditText_2)?.isFocused == true) {
            view?.findViewById<View>(R.id.textInputEditText)?.requestFocus()
        } else if (view?.findViewById<View>(R.id.btn_save)?.isFocused == true) {
            view?.findViewById<View>(R.id.textInputEditText_2)?.requestFocus()
        }
    }

    fun onDpadCenter() {
        Logger.e(this, message = "onDpadCenter")
        if (view?.findViewById<Button>(R.id.btn_save)?.isFocused == true) {
            Logger.e(this@FragmentAddExtensions, message = "On Click")
            if (!view?.findViewById<TextInputEditText>(R.id.textInputEditText_2)?.text.toString().startsWith("http")) {
                showErrorDialog(content = "Đường dẫn không hợp lệ! Vui lòng thử lại")
                return
            }
            disposable.add(
                roomDataBase.extensionsConfig()
                    .insert(
                        ExtensionsConfig(
                            view?.findViewById<TextInputEditText>(R.id.textInputEditText)?.text.toString(),
                            view?.findViewById<TextInputEditText>(R.id.textInputEditText_2)?.text.toString(),
                        )
                    )
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        Logger.d(this@FragmentAddExtensions, message = "Save link success")
                    }, {
                        Logger.e(this@FragmentAddExtensions, exception = it)
                    })
            )
        }
    }


}