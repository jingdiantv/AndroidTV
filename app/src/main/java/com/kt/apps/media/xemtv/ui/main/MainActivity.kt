package com.kt.apps.media.xemtv.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.databinding.ActivityMainBinding
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

/**
 * Loads [DashboardFragment].
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    @Inject
    lateinit var roomDataBase: RoomDataBase

    private val disposable by lazy {
        CompositeDisposable()
    }

    private val tvChannelViewModel by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
    }

    override val layoutRes: Int
        get() = R.layout.activity_main

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initAction(savedInstanceState: Bundle?) {
        disposable.add(
            roomDataBase.extensionsConfig()
                .insert(ExtensionsConfig.test)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Logger.e(this, message = "Complete")
                }, {
                    Logger.e(this, exception = it)
                })
        )

        disposable.add(
            roomDataBase.extensionsConfig()
                .getAll()
                .flatMap {
                    if (it.isEmpty()) {
                        roomDataBase.extensionsConfig()
                            .insert(ExtensionsConfig.test)
                            .andThen(Observable.just(listOf(ExtensionsConfig.test)))
                    } else {
                        Observable.just(it)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (savedInstanceState == null) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.main_browse_fragment, DashboardFragment.newInstance(it))
                            .commitNow()
                    }
                }, {
                    Logger.e(this@MainActivity, exception = it)
                    if (savedInstanceState == null) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.main_browse_fragment, DashboardFragment())
                            .commitNow()
                    }
                })
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
//            supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
//                .takeIf {
//                    it is DashboardFragment
//                }
//                ?.let {
//                    (it as DashboardFragment).onDpadUp()
//                }
//        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
                ?.takeIf {
                    it is DashboardFragment
                }?.let {
                    (it as DashboardFragment).selectPageRowByUri(uri)
                }
        }
    }
}