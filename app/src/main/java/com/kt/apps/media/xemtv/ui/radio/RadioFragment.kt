package com.kt.apps.media.xemtv.ui.radio

import android.content.Intent
import android.os.Parcelable
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import com.kt.apps.core.base.leanback.*
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.adapter.leanback.applyLoading
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.presenter.DashboardTVChannelPresenter
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.details.DetailsActivity
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

class RadioFragment : BaseRowSupportFragment() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }
    private val mRowsAdapter: ArrayObjectAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }

    private var selectedView: ImageCardView? = null

    override fun initView(rootView: View) {
        adapter = mRowsAdapter
        onItemViewSelectedListener = OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
        }

        onItemViewClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            tvChannelViewModel.getLinkStreamForChannel(tvDetail = item as TVChannel)
            selectedView = itemViewHolder.view as ImageCardView
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
                    mRowsAdapter.applyLoading()
                }

                else -> {
                    progressManager.hide()
                }
            }
        }
        tvChannelViewModel.tvWithLinkStreamLiveData
            .observe(viewLifecycleOwner) {
                handleGetTVChannelLinkStream(it)
            }
    }

    private fun handleGetTVChannelLinkStream(it: DataState<TVChannelLinkStream>) {
        when (it) {
            is DataState.Loading -> {
                progressManager.show()
            }
            is DataState.Success -> {
                progressManager.hide()

                val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                intent.putExtra(PlaybackActivity.EXTRA_TV_CHANNEL, it.data)
                intent.putExtra(PlaybackActivity.EXTRA_PLAYBACK_TYPE, PlaybackActivity.Type.RADIO as Parcelable)
                val bundle = try {
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        requireActivity(),
                        selectedView!!.mainImageView,
                        DetailsActivity.SHARED_ELEMENT_NAME
                    ).toBundle()
                } catch (e: Exception) {
                    bundleOf()
                }

                startActivity(intent, bundle)
            }
            is DataState.Error -> {
                progressManager.hide()
                showErrorDialog(content = it.throwable.message)
            }
            else -> {
                progressManager.hide()
            }
        }
        if (it is DataState.Loading) {
            progressManager.show()
        } else {
            progressManager.hide()
        }
    }


    override fun onStop() {
        super.onStop()
        tvChannelViewModel.clearCurrentPlayingChannelState()
    }
}