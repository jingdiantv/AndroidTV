package com.kt.apps.media.xemtv.ui.main

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.HeadersSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.PageRow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.Constants
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class DashboardFragment : BrowseSupportFragment(), HasAndroidInjector {

    private lateinit var mBackgroundManager: BackgroundManager

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var currentPageIdSelected: Long = -1

    private val rowsAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter())
    }

    private val pageRowFactory by lazy {
        DashboardPageRowFactory(BackgroundManager.getInstance(requireActivity())).apply {
            this.onFragmentChangeListener = {
                title = defaultPages[it]
                currentPageIdSelected = it
            }
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        progressBarManager.disableProgressBar()
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainFragmentRegistry.registerFragment(
            PageRow::class.java,
            pageRowFactory
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initView()
        childFragmentManager.addFragmentOnAttachListener { _, fragment ->
            if (fragment is HeadersSupportFragment) {
                headersSupportFragment.lifecycle.addObserver(object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event == Lifecycle.Event.ON_RESUME) {
                            headersSupportFragment.verticalGridView.setBackgroundColor(Color.BLACK)
                            headersSupportFragment.view
                                ?.findViewById<View>(androidx.leanback.R.id.fade_out_edge)
                                ?.apply {
                                    this.setWillNotDraw(true)
                                    val oldLayoutParams = this.layoutParams
                                    oldLayoutParams.width = 5.dpToPx()
                                    this.layoutParams = oldLayoutParams
                                }
                                ?.setBackgroundColor(Color.parseColor("#141414"))
                            headersSupportFragment.lifecycle.removeObserver(this)
                        }
                    }

                })
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAction()
    }

    private fun initView() {
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        title = getString(R.string.app_name)
        prepareEntranceTransition()
        adapter = rowsAdapter
    }

    private fun initAction() {
        activity?.intent?.data?.let {
            selectPageRowByUri(it)
        }
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
        mBackgroundManager.color = Color.BLACK
        brandColor = ContextCompat.getColor(requireContext(), com.kt.skeleton.R.color.skeleton_elements_100)
        searchAffordanceColor

        defaultPages.forEach {
            val header = DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                it.key,
                it.value,
                defaultPagesIcon[it.key]!!
            )
            val pageRow = PageRow(header)
            rowsAdapter.add(pageRow)
        }
        setHeaderPresenterSelector(DashboardIconHeaderPresenterSelector())
        startEntranceTransition()
    }

    fun selectPageRowByUri(uri: Uri) {
        when (uri.scheme) {
            Constants.SCHEME_DEFAULT -> {
                Logger.d(this, message = uri.toString())
                when (uri.host) {
                    Constants.HOST_FOOTBALL -> {
                        setSelectedPosition(1, true)
                    }
                    Constants.HOST_TV -> {
                        setSelectedPosition(0, true)
                    }
                    Constants.HOST_RADIO -> {
                        setSelectedPosition(2, true)
                    }
                }
            }

            else -> {

            }
        }
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    override fun onResume() {
        super.onResume()
        mBackgroundManager.drawable = when (currentPageIdSelected) {
            DashboardPageRowFactory.ROW_FOOTBALL -> {
                ContextCompat.getDrawable(requireContext(), com.kt.apps.core.R.drawable.main_background)
            }
            else -> {
                null
            }
        }
    }

    companion object {
        private val defaultPages by lazy {
            mapOf(
                DashboardPageRowFactory.ROW_TV to "Truyền hình",
                DashboardPageRowFactory.ROW_FOOTBALL to "Bóng đá",
                DashboardPageRowFactory.ROW_RADIO to "Truyền thanh"
            )
        }
        private val defaultPagesIcon by lazy {
            mapOf(
                DashboardPageRowFactory.ROW_TV to R.drawable.ic_tv,
                DashboardPageRowFactory.ROW_FOOTBALL to R.drawable.ic_soccer_ball,
                DashboardPageRowFactory.ROW_RADIO to R.drawable.ic_radio,
            )
        }
    }
}