package com.kt.apps.media.xemtv.ui.main

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.HeadersSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.PageRow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.Constants
import com.kt.apps.core.R
import com.kt.apps.core.base.IKeyCodeHandler
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.gone
import com.kt.apps.media.xemtv.ui.extensions.FragmentAddExtensions
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class DashboardFragment : BrowseSupportFragment(), HasAndroidInjector, IKeyCodeHandler {

    private lateinit var mBackgroundManager: BackgroundManager

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var currentPageIdSelected: Long = -1

    private val rowsAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }

    private var extensionRows: List<ExtensionsConfig>? = null

    private val pageRowFactory by lazy {
        DashboardPageRowFactory(
            BackgroundManager.getInstance(requireActivity()),
            arguments?.getParcelableArray(EXTRA_EXTERNAL_EXTENSIONS)?.toList() as List<ExtensionsConfig>? ?: listOf()
        ).apply {
            this.onFragmentChangeListener = {
                title = if (it == DashboardPageRowFactory.ROW_ADD_EXTENSION) {
                    "Thêm nguồn"
                } else {
                    defaultPages[it] ?: extensionRows?.get(it.toInt())?.sourceName ?: "Test"
                }
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
                            headersSupportFragment.verticalGridView
                                .apply {
                                    this.setBackgroundColor(Color.BLACK)
                                }
                            headersSupportFragment.view
                                ?.findViewById<View>(androidx.leanback.R.id.fade_out_edge)
                                ?.gone()
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
        onItemViewSelectedListener = OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            Logger.e(this, message = row.toString())
            title = if (row.id == DashboardPageRowFactory.ROW_ADD_EXTENSION) {
                startHeadersTransition(false)
                requireActivity().onKeyDown(
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                    )
                )
                "Thêm nguồn"
            } else {
                defaultPages[row.id] ?: extensionRows?.get(row.id.toInt())?.sourceName ?: "Test"
            }
            currentPageIdSelected = row.id
        }

        var pageCount = 0
        defaultPages.forEach {
            val header = DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                it.key,
                it.value,
                defaultPagesIcon[it.key]!!
            )
            val pageRow = PageRow(header)
            rowsAdapter.add(pageRow)
        }
        extensionRows = arguments?.getParcelableArray(EXTRA_EXTERNAL_EXTENSIONS)?.toList() as List<ExtensionsConfig>?
        extensionRows?.forEach {
            rowsAdapter.add(
                PageRow(
                    DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                        pageCount.toLong(),
                        it.sourceName,
                        com.kt.apps.media.xemtv.R.drawable.round_insert_link_24,
                    )
                )
            )
            pageCount++
        }
        rowsAdapter.add(
            PageRow(
                DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                    DashboardPageRowFactory.ROW_ADD_EXTENSION,
                    "Thêm nguồn",
                    com.kt.apps.media.xemtv.R.drawable.round_add_circle_outline_24,
                )
            )
        )
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
                ContextCompat.getDrawable(requireContext(), R.drawable.tv_bg)
            }
        }
    }

    override fun onDpadCenter() {

        if (mainFragment is FragmentAddExtensions) {
            (mainFragment as FragmentAddExtensions)
                .onDpadCenter()
        }
    }

    override fun onDpadDown() {

    }

    override fun onDpadUp() {
        if (mainFragment is FragmentAddExtensions) {
            (mainFragment as FragmentAddExtensions)
                .onDpadUp()
        }
    }

    override fun onDpadLeft() {

    }

    override fun onDpadRight() {

    }

    override fun onKeyCodeChannelUp() {

    }

    override fun onKeyCodeChannelDown() {

    }

    override fun onKeyCodeMediaPrevious() {

    }

    override fun onKeyCodeMediaNext() {

    }

    override fun onKeyCodeVolumeUp() {

    }

    override fun onKeyCodeVolumeDown() {

    }

    override fun onKeyCodePause() {

    }

    override fun onKeyCodePlay() {

    }

    override fun onKeyCodeMenu() {

    }

    companion object {
        private const val EXTRA_EXTERNAL_EXTENSIONS = "extra:external"
        val defaultPages by lazy {
            mapOf(
                DashboardPageRowFactory.ROW_TV to "Truyền hình",
//                DashboardPageRowFactory.ROW_FOOTBALL to "Bóng đá",
                DashboardPageRowFactory.ROW_RADIO to "Phát thanh",
//                DashboardPageRowFactory.ROW_ADD_EXTENSION to "Thêm nguồn",
            )
        }
        private val defaultPagesIcon by lazy {
            mapOf(
                DashboardPageRowFactory.ROW_TV to R.drawable.ic_tv,
                DashboardPageRowFactory.ROW_FOOTBALL to R.drawable.ic_soccer_ball,
                DashboardPageRowFactory.ROW_RADIO to R.drawable.ic_radio,
//                DashboardPageRowFactory.ROW_ADD_EXTENSION to com.kt.apps.media.xemtv.R.drawable.round_add_circle_outline_24,
            )
        }

        fun newInstance(extensionsConfigs: List<ExtensionsConfig>) = DashboardFragment().apply {
            arguments = bundleOf(
                EXTRA_EXTERNAL_EXTENSIONS to extensionsConfigs.toTypedArray()
            )
        }
    }
}