package com.kt.apps.media.xemtv.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.PageRow
import androidx.lifecycle.ViewModelProvider
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

    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }

    private val rowsAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter())
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backgroundManager = BackgroundManager.getInstance(requireActivity())
        mainFragmentRegistry.registerFragment(
            PageRow::class.java,
            DashboardPageRowFactory(backgroundManager)
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initView()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAction()
    }

    private fun initView() {
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireContext(), com.kt.skeleton.R.color.primary_black_gray)
        title = getString(R.string.app_name)
        prepareEntranceTransition()
        adapter = rowsAdapter
    }

    private fun initAction() {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
        defaultPages.forEach {
            val header = DashboardIconHeaderPresenterSelector.HeaderIconPresenter.HeaderItemWithIcon(
                it.key,
                it.value,
                R.drawable.app_icon
            )
            val pageRow = PageRow(header)
            rowsAdapter.add(pageRow)
        }
        setHeaderPresenterSelector(DashboardIconHeaderPresenterSelector())
        startEntranceTransition()
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    companion object {
        private val defaultPages by lazy {
            mapOf(
                DashboardPageRowFactory.ROW_TV to "Truyền hình",
                DashboardPageRowFactory.ROW_FOOTBALL to "Bóng đá",
                DashboardPageRowFactory.ROW_SETTING to "Video",
            )
        }
    }
}