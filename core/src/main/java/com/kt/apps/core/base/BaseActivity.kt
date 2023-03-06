package com.kt.apps.core.base

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.core.utils.updateLocale
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

abstract class BaseActivity<T : ViewDataBinding> : FragmentActivity(), HasAndroidInjector {
    abstract val layoutRes: Int
    abstract fun initView(savedInstanceState: Bundle?)
    abstract fun initAction(savedInstanceState: Bundle?)
    lateinit var binding: T
    private var doubleBackToFinish = false
    private val updateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val appUpdateInfoTask by lazy { updateManager.appUpdateInfo }
    private val appUpdateListener by lazy {
        InstallStateUpdatedListener { state ->

            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val byteDownload = state.bytesDownloaded()
                    val totalBytesDownload = state.totalBytesToDownload()
                }
                InstallStatus.DOWNLOADED -> {

                }
                else -> {

                }
            }
        }
    }


    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any?>? {
        return androidInjector
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        window.decorView.setBackgroundColor(Color.WHITE)
        super.onCreate(savedInstanceState)
        updateLocale()
        appUpdateInfoTask.addOnSuccessListener { updateInfo ->
            if (updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                updateManager.registerListener(appUpdateListener)
                updateManager.startUpdateFlowForResult(
                    updateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }

        binding = DataBindingUtil.setContentView(this, layoutRes)
        initView(savedInstanceState)
        initAction(savedInstanceState)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    fun doubleBackToFinish() {
        if (doubleBackToFinish) {
            finish()
        } else {
            Toast.makeText(
                this,
                "Nhấn back lần nữa để thoát",
                Toast.LENGTH_SHORT
            ).show()
        }
        doubleBackToFinish = true
        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToFinish = false
        }, 2000)

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        currentFocus?.let { currentFocusView ->
            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_MOVE && currentFocusView is EditText
                && !currentFocusView::class.java.name.startsWith("android.webkit.")
            ) {
                val sourceCoordinator = IntArray(2)
                currentFocusView.getLocationOnScreen(sourceCoordinator)
                val x = ev.rawX + currentFocusView.left - sourceCoordinator[0]
                val y = ev.rawY + currentFocusView.top - sourceCoordinator[1]
                if (x < currentFocusView.left || x > currentFocusView.right || y < currentFocusView.top || y > currentFocusView.bottom) {
                    currentFocusView.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                    imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                showSuccessDialog({}, "App update fail!")
            } else {
                updateManager.unregisterListener(appUpdateListener)
                showSuccessDialog({}, "App update success")
            }
        }
    }

    fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            binding.root,
            "An update has just been downloaded.",
            Snackbar.LENGTH_SHORT
        ).apply {
            setAction("RESTART") { updateManager.completeUpdate() }
            setActionTextColor(resources.getColor(com.kt.skeleton.R.color.white))
            show()
        }
    }

    companion object {
        private const val UPDATE_REQUEST_CODE = 102
    }
}
