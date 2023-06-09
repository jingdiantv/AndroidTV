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
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

abstract class BaseTabLayoutFragment : BaseRowSupportFragment() {
    abstract val currentPage: Int
    abstract val tabLayout: LeanbackTabLayout
    abstract fun requestFocusChildContent(): View
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

    private var type = PlaybackActivity.Type.TV

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
            totalList.add(FragmentTVDashboard.FILTER_TOTAL)
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
            return FragmentTVDashboard.newInstance(totalList[position], type, mMainFragmentAdapter)
        }

        override fun getPageTitle(position: Int): CharSequence {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = requireArguments().getParcelable(EXTRA_TYPE) ?: PlaybackActivity.Type.TV
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
                    val totalList = it.data.filter(filterByType())
                    pagerAdapter.onRefresh(totalList)
                    tabLayout.setupWithViewPager(viewPager, true)
                }

                else -> {

                }
            }
        })

    }

    private fun filterByType() = { channel: TVChannel ->
        if (type == PlaybackActivity.Type.TV) {
            !channel.isRadio
        } else {
            channel.isRadio
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(arguments)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState?.containsKey(EXTRA_TYPE) == true) {
            type = savedInstanceState.getParcelable(EXTRA_TYPE) ?: PlaybackActivity.Type.TV
        }
    }

    override fun requestFocusChildContent(): View {
        return viewPager
    }

    companion object {
        private const val EXTRA_TYPE = "extra:type"
        fun newInstance(dashboardType: PlaybackActivity.Type) = FragmentTVDashboardNew().apply {
            arguments = bundleOf(
                EXTRA_TYPE to dashboardType
            )
        }
    }

}