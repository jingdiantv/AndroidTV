package com.kt.apps.media.xemtv.ui.radio

import android.view.View
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.xemtv.presenter.DashboardTVChannelPresenter
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import javax.inject.Inject

class RadioFragment : BaseRowSupportFragment() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }
    private val mRowsAdapter: ArrayObjectAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter())
    }

    override fun initView(rootView: View) {
        adapter = mRowsAdapter
        onItemViewSelectedListener = OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
        }

        onItemViewClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            tvChannelViewModel.getLinkStreamForChannel(tvDetail = item as TVChannel)
        }
    }

    override fun initAction(rootView: View) {
        if (tvChannelViewModel.tvChannelLiveData.value !is DataState.Success) {
            tvChannelViewModel.getListTVChannel(false)
        }
        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    mRowsAdapter.clear()
                    progressManager.hide()
                    val radio = it.data.filter {
                        it.isRadio
                    }.groupBy {
                        it.tvGroup
                    }
                    var currentId = 0L
                    for ((groupName, radioChannelList) in radio) {
                        val headerItem = HeaderItem(currentId, groupName)
                        val rowAdapter = ArrayObjectAdapter(DashboardTVChannelPresenter())
                        for (radioChannel in radioChannelList) {
                            rowAdapter.add(radioChannel)
                        }
                        mRowsAdapter.add(ListRow(headerItem, rowAdapter))
                        currentId++
                    }
                    mainFragmentAdapter.fragmentHost.notifyDataReady(mainFragmentAdapter)
                }

                is DataState.Loading -> {
                    progressManager.show()
                }

                else -> {
                    progressManager.hide()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        tvChannelViewModel.clearCurrentPlayingChannelState()
    }
}