package com.kt.apps.media.xemtv.ui.extensions

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.transition.Scene
import com.google.android.material.button.MaterialButton
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.utils.leanback.findCurrentFocusedPosition
import com.kt.apps.core.utils.leanback.findCurrentFocusedView
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.tv.BaseTabLayoutFragment
import javax.inject.Inject

class FragmentDashboardExtensions : BaseTabLayoutFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }


    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[ExtensionsViewModel::class.java]
    }

    private val viewPager by lazy {
        requireView().findViewById<LeanbackViewPager>(R.id.view_pager)
    }

    override val currentPage: Int
        get() = requireView().findViewById<LeanbackViewPager>(R.id.view_pager).currentItem

    override val tabLayout: LeanbackTabLayout
        get() = requireView().findViewById(R.id.tab_layout)

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_extensions_dashboard
    }

    private val pagerAdapter by lazy {
        ExtensionsChannelViewPager(childFragmentManager)
    }

    private var _btnAddSource: MaterialButton? = null

    override fun initView(rootView: View) {
        _btnAddSource = rootView.findViewById(R.id.btn_add_source)
    }

    override fun initAction(rootView: View) {
        extensionsViewModel.loadAllListExtensionsChannelConfig(true)
        val emptyScene: Scene = Scene.getSceneForLayout(
            rootView as ViewGroup,
            R.layout.fragment_extensions_dashboard_empty_sence,
            requireContext()
        )
        val anotherScene: Scene =
            Scene.getSceneForLayout(rootView, R.layout.fragment_extensions_dashboard, requireContext())

        viewPager.adapter = pagerAdapter
        setAlignment(mAlignedTop)

        _btnAddSource?.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    com.kt.skeleton.R.anim.fade_in, com.kt.skeleton.R.anim.fade_out,
                    com.kt.skeleton.R.anim.fade_in, com.kt.skeleton.R.anim.fade_out
                )
                .add(android.R.id.content, FragmentAddExtensions(), FragmentDashboardExtensions::class.java.name)
                .addToBackStack(FragmentDashboardExtensions::class.java.name)
                .commit()
        }

        extensionsViewModel.totalExtensionsConfig.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    val listConfig = it.data
                    pagerAdapter.onRefresh(listConfig)
                    if (listConfig.size > 1) {
                        viewPager.currentItem = 0
                    }
                    tabLayout.setupWithViewPager(viewPager, true)
                }

                is DataState.Loading -> {

                }

                else -> {

                }
            }
        }

    }

    fun onFocusSearch(
        focused: View?,
        direction: Int
    ): View? {
        if (focused == _btnAddSource) {
            if (direction == View.FOCUS_RIGHT) {
                if (tabLayout.tabCount > 0) {
                    return tabLayout.getTabAt(0)?.view
                }
            }
        }

        if (focused == tabLayout.findCurrentFocusedView()) {
            val currentFocus = tabLayout.findCurrentFocusedPosition()
            val tabCount = tabLayout.tabCount
            if (direction == View.FOCUS_LEFT) {
                if (currentFocus == 0) {
                    return _btnAddSource
                }
            } else if (direction == View.FOCUS_RIGHT) {
                return if (currentFocus == tabCount - 1) {
                    _btnAddSource
                } else {
                    tabLayout.getTabAt((currentFocus + 1) % tabLayout.tabCount)?.view
                }
            }
        }

        throw Throwable("Return to parent focus search")
    }

    class ExtensionsChannelViewPager(fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager) {
        private val totalList by lazy {
            mutableListOf<ExtensionsConfig>()
        }

        fun onRefresh(extensionsConfigs: List<ExtensionsConfig>) {
            totalList.clear()
            totalList.addAll(extensionsConfigs)
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return totalList.size
        }

        override fun getItem(position: Int): Fragment {
            return FragmentExtensions.newInstance(totalList[position].sourceUrl)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return totalList[position].sourceName
        }
    }

}