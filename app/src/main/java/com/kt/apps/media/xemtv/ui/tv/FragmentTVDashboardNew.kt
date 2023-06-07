package com.kt.apps.media.xemtv.ui.tv

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.leanback.BrowseSupportFragment.FragmentHostImpl
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import javax.inject.Inject

abstract class BaseTabLayoutFragment : BaseRowSupportFragment() {
    abstract val currentPage: Int
    abstract val tabLayout: LeanbackTabLayout
}

class FragmentTVDashboardNew : BaseTabLayoutFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory


    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }

    private val viewPager by lazy {
        requireView().findViewById<LeanbackViewPager>(R.id.view_pager)
    }

    override val currentPage: Int
        get() = requireView().findViewById<LeanbackViewPager>(R.id.view_pager).currentItem

    override val tabLayout: LeanbackTabLayout
        get() = requireView().findViewById(R.id.tab_layout)

    inner class TVViewPager(fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager) {
        private val totalList by lazy {
            mutableListOf<String>()
        }

        fun onRefresh(listTvChannel: List<TVChannel>) {
            totalList.clear()
            totalList.add("Tất cả")
            totalList.addAll(
                listTvChannel.groupBy {
                    it.tvGroup
                }.keys
            )
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return totalList.size
        }

        override fun getItem(position: Int): Fragment {
            return if (position == 0) {
                FragmentTVGrid().apply {
                    this.mainFragmentAdapter.setFragmentHost(this@FragmentTVDashboardNew.mainFragmentAdapter.fragmentHost as FragmentHostImpl?)
                }
            } else {
                FragmentTVDashboard().apply {
                    mMainFragmentAdapter = this@FragmentTVDashboardNew.mMainFragmentAdapter
                    this.arguments = bundleOf(
                        "filterGroup" to totalList[position]
                    )
                }
            }
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return if (position == 0) {
                totalList[position]
            } else {
                TVChannelGroup.valueOf(totalList[position]).value
            }
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_tv_dashboard_new
    }

    private val pagerAdapter by lazy {
        TVViewPager(childFragmentManager)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager.adapter = pagerAdapter
        setAlignment(mAlignedTop)
    }

    override fun initView(rootView: View) {

    }

    override fun initAction(rootView: View) {
        tvChannelViewModel.getListTVChannel(false)
        mainFragmentAdapter.fragmentHost!!.notifyViewCreated(mainFragmentAdapter)
        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                is DataState.Success -> {
                    pagerAdapter.onRefresh(it.data)
                    tabLayout.setupWithViewPager(viewPager, true)
                }

                else -> {

                }
            }
        })

    }

}