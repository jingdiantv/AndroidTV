package com.kt.apps.media.xemtv.ui.main

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.setPadding
import androidx.leanback.app.*
import androidx.leanback.widget.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.Constants
import com.kt.apps.core.R
import com.kt.apps.core.base.IKeyCodeHandler
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.gone
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.media.xemtv.BuildConfig
import com.kt.apps.media.xemtv.ui.extensions.FragmentAddExtensions
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class DashboardFragment : BrowseSupportFragment(), HasAndroidInjector, IKeyCodeHandler {

    private lateinit var mBackgroundManager: BackgroundManager

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var roomDataBase: RoomDataBase

    private val disposable by lazy {
        CompositeDisposable()
    }

    private var currentPageIdSelected: Long = -1

    private val rowsAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }

    private var extensionRows: MutableList<ExtensionsConfig>? = null

    private val pageRowFactory by lazy {
        DashboardPageRowFactory(
            BackgroundManager.getInstance(requireActivity()),
            arguments?.getParcelableArray(EXTRA_EXTERNAL_EXTENSIONS)?.toList() as List<ExtensionsConfig>? ?: listOf()
        ).apply {
            this.onFragmentChangeListener = {
                title = if (it == DashboardPageRowFactory.ROW_ADD_EXTENSION) {
                    "Thêm nguồn"
                } else {
                    defaultPages[it] ?: try {
                        extensionRows?.get(it.toInt())?.sourceName ?: "Test"
                    } catch (e: Exception) {
                        "Test"
                    }
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
                        } else if (event == Lifecycle.Event.ON_START) {
                            headersSupportFragment.view?.findViewById<RelativeLayout>(androidx.leanback.R.id.browse_headers_root)
                                ?.apply {
                                    val textView = TextView(requireContext())
                                    textView.setTextAppearance(com.google.android.exoplayer2.ext.leanback.R.style.TextAppearance_Leanback_DetailsDescriptionBody)
                                    val layoutParams = RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.MATCH_PARENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                                    textView.gravity = Gravity.CENTER
                                    textView.setPadding(16.dpToPx())
                                    textView.alpha = 0.4f
                                    layoutParams.alignWithParent = true
                                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                                    textView.layoutParams = layoutParams
                                    textView.text = getDisplayVersionName()
                                    this.addView(textView)
                                }
                        }
                    }

                })
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun getDisplayVersionName(): String {
        return getString(com.kt.apps.core.R.string.app_name) + "." + BuildConfig.VERSION_CODE + "-" + BuildConfig.BUILD_TYPE
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
        Logger.e(this, message = "initAction")
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

        defaultPages.forEach {
            val header = DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                it.key,
                it.value,
                defaultPagesIcon[it.key]!!
            )
            val pageRow = PageRow(header)
            rowsAdapter.add(pageRow)
        }
        extensionRows =
            arguments?.getParcelableArray(EXTRA_EXTERNAL_EXTENSIONS)?.toMutableList() as MutableList<ExtensionsConfig>?
        disposable.add(
            roomDataBase.extensionsConfig()
                .getAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    pageRowFactory.listExtensions = it
                    extensionRows = it.toMutableList()
                    addExtensionsPage()
                    Logger.d(this@DashboardFragment, message = "addExtensionsPage")
                    disposable.clear()
                }, {
                })
        )
        setHeaderPresenterSelector(DashboardIconHeaderPresenterSelector().apply {
            this.onHeaderLongClickedListener = { headerId ->
                Logger.d(this@DashboardFragment, message = "$headerId")
                val index = headerId.toInt()
                try {
                    extensionRows?.get(index)?.let {
                        val stepFragment = DeleteSourceFragment(
                            it, progressBarManager,
                            disposable,
                            roomDataBase
                        ) {
                            this@DashboardFragment.rowsAdapter.clear()
                            extensionRows!!.removeAt(index)
                            defaultPages.forEach {
                                val header =
                                    DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                                        it.key,
                                        it.value,
                                        defaultPagesIcon[it.key]!!
                                    )
                                val pageRow = PageRow(header)
                                rowsAdapter.add(pageRow)
                            }
                            addExtensionsPage()
                        }

                        val fragment = requireActivity().supportFragmentManager
                            .findFragmentByTag("leanBackGuidedStepSupportFragment")

                        if (fragment != null) {
                            requireActivity().supportFragmentManager
                                .beginTransaction()
                                .replace(android.R.id.content, stepFragment, "leanBackGuidedStepSupportFragment")
                                .commit()
                        } else {
                            GuidedStepSupportFragment.add(
                                requireActivity().supportFragmentManager,
                                stepFragment,
                                android.R.id.content
                            )
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(this, exception = e)
                }
            }
        })
        startEntranceTransition()
    }

    class DeleteSourceFragment(
        val extensions: ExtensionsConfig,
        val progressBarManager: ProgressBarManager,
        val disposable: CompositeDisposable,
        val roomDataBase: RoomDataBase,
        val onDeleteSuccess: () -> Unit,

        ) : GuidedStepSupportFragment() {

        override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
            return GuidanceStylist.Guidance(
                "Xoá nguồn: ${extensions.sourceName}",
                "Sau khi xoá, nguồn kênh sẽ không còn tồn tại nữa!",
                "",
                ContextCompat.getDrawable(
                    requireContext(),
                    com.kt.apps.media.xemtv.R.drawable.round_insert_link_64
                )
            )
        }

        override fun onGuidedActionClicked(action: GuidedAction?) {
            super.onGuidedActionClicked(action)
            when (action?.id) {
                1L -> {
                    progressBarManager.show()
                    disposable.add(
                        roomDataBase.extensionsConfig()
                            .delete(extensions)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                progressBarManager.hide()
                                requireActivity().supportFragmentManager
                                    .beginTransaction()
                                    .remove(this)
                                    .commit()
                                onDeleteSuccess()
                                showSuccessDialog(content = "Xoá nguồn kênh thành công")
                            }, {
                                Logger.e(this@DeleteSourceFragment, exception = it)
                                progressBarManager.hide()
                                showSuccessDialog(content = "Xoá nguồn kênh thất bại")
                            })
                    )
                }

                2L -> {
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .remove(this)
                        .commit()
                }
            }
        }

        override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
            super.onCreateActions(actions, savedInstanceState)
            actions.add(GuidedAction.Builder()
                .id(1)
                .title("Có")
                .description("Xoá nguồn kênh")
                .build())

            actions.add(GuidedAction.Builder()
                .id(2)
                .title("Huỷ")
                .build())
        }
    }

    fun onAddExtensionsPage(extensions: ExtensionsConfig) {
        val finalItemIndex = (extensionRows?.size ?: 0) + defaultPages.size - 1
        val totalPage = extensionRows?.toMutableList() ?: mutableListOf()
        val currentExistedItem = totalPage.findLast {
            it.sourceUrl == extensions.sourceUrl
        }

        if (currentExistedItem != null) {
            val index = totalPage.indexOf(currentExistedItem)
            totalPage[index] = extensions
            rowsAdapter.replace(
                index + 2,
                PageRow(
                    DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                        (totalPage.size - 2).toLong(),
                        extensions.sourceName,
                        com.kt.apps.media.xemtv.R.drawable.round_insert_link_24,
                    )
                )
            )
        } else {
            totalPage.add(extensions)
            rowsAdapter.replace(
                finalItemIndex,
                PageRow(
                    DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                        (totalPage.size - 1).toLong(),
                        extensions.sourceName,
                        com.kt.apps.media.xemtv.R.drawable.round_insert_link_24,
                    )
                )
            )
            rowsAdapter.add(getAddSourcePageRow())
        }
        extensionRows = totalPage
        pageRowFactory.listExtensions = totalPage
    }

    private fun addExtensionsPage() {
        var pageCount = 0

        extensionRows?.forEachIndexed { index, extensionsConfig ->
            val pageRow = PageRow(
                DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                    pageCount.toLong(),
                    extensionsConfig.sourceName,
                    com.kt.apps.media.xemtv.R.drawable.round_insert_link_24,
                )
            )
            if (index == extensionRows!!.size - 1) {
                rowsAdapter.replace(2, pageRow)
                rowsAdapter.add(
                    getAddSourcePageRow()
                )
            } else {
                rowsAdapter.add(pageRow)
            }
            pageCount++
        }
    }

    private fun getAddSourcePageRow() = PageRow(
        DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
            DashboardPageRowFactory.ROW_ADD_EXTENSION,
            "Thêm nguồn",
            com.kt.apps.media.xemtv.R.drawable.round_add_circle_outline_24,
        )
    )

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
                ContextCompat.getDrawable(requireContext(), R.drawable.main_background)
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
                DashboardPageRowFactory.ROW_RADIO to "Phát thanh",
                DashboardPageRowFactory.ROW_ADD_EXTENSION to "Thêm nguồn",
            )
        }
        private val defaultPagesIcon by lazy {
            mapOf(
                DashboardPageRowFactory.ROW_TV to R.drawable.ic_tv,
                DashboardPageRowFactory.ROW_FOOTBALL to R.drawable.ic_soccer_ball,
                DashboardPageRowFactory.ROW_RADIO to R.drawable.ic_radio,
                DashboardPageRowFactory.ROW_ADD_EXTENSION to com.kt.apps.media.xemtv.R.drawable.round_add_circle_outline_24,
            )
        }

        fun newInstance(extensionsConfigs: List<ExtensionsConfig>) = DashboardFragment().apply {
            arguments = bundleOf(
                EXTRA_EXTERNAL_EXTENSIONS to extensionsConfigs.toTypedArray()
            )
        }
    }
}