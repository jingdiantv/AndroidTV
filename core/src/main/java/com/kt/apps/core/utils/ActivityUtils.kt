package com.kt.apps.core.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cn.pedant.SweetAlert.SweetAlertDialog
import com.kt.apps.core.logging.Logger
import java.util.*

fun Context.updateLocale(language: String = "vi") {
    val config = Configuration()
    val locale = Locale(language)
    config.setLocale(locale)
    Locale.setDefault(locale)
    createConfigurationContext(config)
}

fun Fragment.hideKeyboard() {
    requireActivity().hideKeyboard()
}
fun Activity.hideKeyboard() {
    currentFocus?.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(it.windowToken, 0)
    }

}

fun Fragment.showSuccessDialog(
    onSuccessListener: (() -> Unit?)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = true

) {
    requireActivity().showSweetDialog(SweetAlertDialog.SUCCESS_TYPE, onSuccessListener, content, delayMillis, autoDismiss)
}

fun Fragment.showErrorDialog(
    onSuccessListener: (() -> Unit)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
) {
    if (this.isDetached || this.isHidden) {
        return
    }
    val successAlert = SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE)
        .showCancelButton(false)
        .hideConfirmButton()

    successAlert.showContentText(content != null)
    successAlert.setCancelable(true)
    successAlert.contentText = content
    successAlert.titleText = null
    successAlert.confirmText = null
    successAlert.setBackground(ColorDrawable(Color.TRANSPARENT))
    successAlert.show()
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when(event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    Logger.d(this, message = "OnPauseCalled")
                    successAlert.dismissWithAnimation()
                    lifecycle.removeObserver(this)
                }

                else -> {

                }
            }
        }
    })
    Handler(Looper.getMainLooper()).postDelayed({ onSuccessListener?.let { it() } }, (delayMillis ?: 1900).toLong())
}

fun Activity.showSuccessDialog(
    onSuccessListener: (() -> Unit?)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = true
) {
    try {
        showSweetDialog(SweetAlertDialog.SUCCESS_TYPE, onSuccessListener, content, delayMillis, autoDismiss)
    } catch (_: Exception) {
    }
}

fun Activity.showErrorDialog(
    onSuccessListener: (() -> Unit)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = false
) {
    try {
        showSweetDialog(SweetAlertDialog.ERROR_TYPE, onSuccessListener, content, delayMillis, autoDismiss)
    } catch (_: Exception) {
    }
}
@JvmOverloads
fun Activity.showSweetDialog(
    type: Int,
    onSuccessListener: (() -> Unit?)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = false
) {
    val successAlert = SweetAlertDialog(this, type)
        .showCancelButton(false)
        .hideConfirmButton()

    successAlert.showContentText(content != null)
    successAlert.setCancelable(!autoDismiss)
    successAlert.contentText = content
    successAlert.titleText = null
    successAlert.confirmText = null
    successAlert.setBackground(ColorDrawable(Color.TRANSPARENT))

    successAlert.show()
    if (autoDismiss) {
        Handler(Looper.getMainLooper()).postDelayed({ successAlert.dismissWithAnimation() }, 1500)
    } else {
        if (this is FragmentActivity) {
            lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when(event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            try {
                                Logger.d(this, message = "OnPauseCalled")
                                successAlert.dismissWithAnimation()
                                lifecycle.removeObserver(this)
                            } catch (_: Exception) {
                            }
                        }

                        else -> {

                        }
                    }
                }
            })
        }
    }
    Handler(Looper.getMainLooper()).postDelayed({ onSuccessListener?.let { it() } }, (delayMillis ?: 1900).toLong())
}
