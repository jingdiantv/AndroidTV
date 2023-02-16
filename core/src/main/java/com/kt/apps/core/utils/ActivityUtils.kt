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
import cn.pedant.SweetAlert.SweetAlertDialog
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
    requireActivity().showSweetDialog(SweetAlertDialog.ERROR_TYPE, onSuccessListener, content, delayMillis)
}

fun Activity.showSuccessDialog(
    onSuccessListener: (() -> Unit?)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = true
) {
    showSweetDialog(SweetAlertDialog.SUCCESS_TYPE, onSuccessListener, content, delayMillis, autoDismiss)
}

fun Activity.showErrorDialog(
    onSuccessListener: (() -> Unit)? = null,
    content: String? = null,
    delayMillis: Int? = 1900,
    autoDismiss: Boolean = false
) {
    showSweetDialog(SweetAlertDialog.ERROR_TYPE, onSuccessListener, content, delayMillis, autoDismiss)
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
    }
    Handler(Looper.getMainLooper()).postDelayed({ onSuccessListener?.let { it() } }, (delayMillis ?: 1900).toLong())
}
