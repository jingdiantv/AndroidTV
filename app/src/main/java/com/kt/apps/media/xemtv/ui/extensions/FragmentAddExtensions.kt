package com.kt.apps.media.xemtv.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.media.xemtv.R
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import javax.inject.Inject


class FragmentAddExtensions : BaseRowSupportFragment() {

    @Inject
    lateinit var parserExtensionsSource: ParserExtensionsSource

    @Inject
    lateinit var roomDataBase: RoomDataBase
    private val disposablecontainer by lazy {
        CompositeDisposable()
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[ExtensionsViewModel::class.java]
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_add_extensions, container, false)
        initView(rootView)
        progressManager.initialDelay = 500
        progressManager.setRootView(requireActivity().findViewById(android.R.id.content))
        return rootView
    }

    override fun initView(rootView: View) {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Logger.e(this, message = "onViewCreated")
        initAction(view)
        view.findViewById<TextInputEditText>(R.id.textInputEditText).setupFocusChangeShowKeyboard()
        view.findViewById<TextInputEditText>(R.id.textInputEditText_2).setupFocusChangeShowKeyboard()
        view.findViewById<TextInputEditText>(R.id.textInputEditText).requestFocus()
    }

    private fun TextInputEditText.setupFocusChangeShowKeyboard() {
        val oldFocusChange = onFocusChangeListener
        setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val imm: InputMethodManager = requireContext().getSystemService(
                    InputMethodManager::class.java
                )
                if (imm.isActive(v)) {
                    imm.showSoftInput(v, 0)
                }
            }
            oldFocusChange?.onFocusChange(v, hasFocus)
        }
    }

    override fun initAction(rootView: View) {
        rootView.findViewById<TextInputEditText>(R.id.textInputEditText_2)
            .setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val imm: InputMethodManager = requireContext().getSystemService(
                        InputMethodManager::class.java
                    )
                    if (imm.isActive(v)) {
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                    addExtensionsSource()
                }
                return@setOnEditorActionListener true
            }

        rootView.findViewById<TextInputEditText>(R.id.textInputEditText)
            .setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    rootView.findViewById<TextInputEditText?>(R.id.textInputEditText_2)
                        ?.requestFocus()
                }
                return@setOnEditorActionListener true
            }

        rootView.findViewById<View>(R.id.btn_save)!!.setOnClickListener {
            addExtensionsSource()
        }
    }

    fun onDpadUp() {
        if (view?.findViewById<View>(R.id.textInputEditText_2)?.isFocused == true) {
            view?.findViewById<View>(R.id.textInputEditText)?.requestFocus()
        } else if (view?.findViewById<View>(R.id.btn_save)?.isFocused == true) {
            view?.findViewById<View>(R.id.textInputEditText_2)?.requestFocus()
        }
    }

    fun onDpadCenter() {
    }

    private fun addExtensionsSource() {
        if (view?.findViewById<TextInputEditText>(R.id.textInputEditText)?.text.isNullOrBlank()) {
            showErrorDialog(content = "Tên nguồn không được bỏ trống")
            return
        }
        val sourceUrl = view?.findViewById<TextInputLayout>(R.id.textInputLayout_2)?.prefixText.toString() +
                view?.findViewById<TextInputEditText>(R.id.textInputEditText_2)?.text.toString()
        if (!sourceUrl.startsWith("http")) {
            showErrorDialog(content = "Đường dẫn không hợp lệ! Đường dẫn phải phải bắt đầu bằng: \"http\"")
            return
        }
        val extensionsConfig = ExtensionsConfig(
            view?.findViewById<TextInputEditText>(R.id.textInputEditText)?.text.toString(),
            sourceUrl,
        )
        progressManager.show()
        val disposable = parserExtensionsSource.parseFromRemoteMaybe(extensionsConfig)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable {
                if (it.isNotEmpty()) {
                    roomDataBase.extensionsConfig()
                        .insert(extensionsConfig)
                } else {
                    Completable.error(Throwable("Data empty"))
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (this.isHidden || this.isDetached) {
                    return@subscribe
                }
                progressManager.hide()
                showSuccessDialog(
                    content = "Thêm nguồn kênh thành công!" +
                            "\r\nKhởi động lại ứng dụng để kiểm tra nguồn kênh",
                    onSuccessListener = {
                        if (!this.isDetached) {
                            requireActivity().supportFragmentManager
                                .popBackStack()
                        }
                    }
                )
                Logger.d(this@FragmentAddExtensions, message = "Save link success")
                extensionsViewModel.loadAllListExtensionsChannelConfig(true)
            }, {
                if (this.isHidden || this.isDetached) {
                    return@subscribe
                }
                progressManager.hide()
                showErrorDialog(content = "Định dạng nguồn kênh chưa được hỗ trợ!")
                Logger.e(this@FragmentAddExtensions, exception = it)
            })
        disposablecontainer.add(disposable)
    }

    private fun startServer() {
        disposablecontainer.add(
            Observable.create<Any> {
                val serverSocket = ServerSocket(4444)
                val clientSocket = serverSocket.accept()
                val outputStream = PrintWriter(clientSocket.getOutputStream(), true)
                val inputStream = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                var inputLine: String
                var outputLine: String

                while (true) {

                    Thread.sleep(1000)

                }

            }
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe({

                }, {

                }, {

                })
        )
    }

}