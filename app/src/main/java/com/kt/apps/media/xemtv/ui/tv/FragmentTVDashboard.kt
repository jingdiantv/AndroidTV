package com.kt.apps.media.xemtv.ui.tv

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.adapter.leanback.applyLoading
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.presenter.DashboardTVChannelPresenter
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.details.DetailsActivity
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import java.util.*
import javax.inject.Inject

class FragmentTVDashboard : BaseRowSupportFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }
    private val mRowsAdapter: ArrayObjectAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter())
    }
    private var selectedView: ImageCardView? = null
    private var mBackgroundUri: String? = null
    private var mBackgroundTimer: Timer? = null

    override fun initView(rootView: View) {

    }

    override fun initAction(rootView: View) {
        adapter = mRowsAdapter
        if (tvChannelViewModel.tvChannelLiveData.value !is DataState.Success) {
            tvChannelViewModel.getListTVChannel(false)
        }
        onItemViewSelectedListener = OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            if (item is TVChannel) {
                mBackgroundUri = item.logoChannel
                startBackgroundTimer()
            }
        }

        onItemViewClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            tvChannelViewModel.getLinkStreamForChannel(tvDetail = item as TVChannel)
            selectedView = itemViewHolder.view as ImageCardView
        }

        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    mRowsAdapter.clear()
                    val channelWithCategory = it.data.groupBy {
                        it.tvGroup
                    }
                    val dashboardTVChannelPresenter = DashboardTVChannelPresenter()
                    for ((group, channelList) in channelWithCategory) {
                        val headerItem = HeaderItem(group)
                        val adapter = ArrayObjectAdapter(dashboardTVChannelPresenter)
                        for (channel in channelList) {
                            adapter.add(channel)
                        }
                        mRowsAdapter.add(ListRow(headerItem, adapter))
                    }
                    mainFragmentAdapter.fragmentHost.notifyDataReady(mainFragmentAdapter)
                }
                is DataState.Loading -> {
                    mRowsAdapter.applyLoading(R.layout.item_tv_loading_presenter)
                    mainFragmentAdapter.fragmentHost.notifyDataReady(mainFragmentAdapter)
                }
                else -> {
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
            }
            is DataState.Success -> {
                val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                intent.putExtra(PlaybackActivity.EXTRA_TV_CHANNEL, it.data)
                intent.putExtra(PlaybackActivity.EXTRA_PLAYBACK_TYPE, PlaybackActivity.Type.TV as Parcelable)
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
                showErrorDialog(content = it.throwable.message)
            }
            else -> {
            }
        }
        if (it is DataState.Loading) {
            progressManager.show()
        } else {
            progressManager.hide()
        }
    }

    override fun onStop() {
        tvChannelViewModel.clearCurrentPlayingChannelState()
        super.onStop()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), 300)
    }

    private val mHandler = Handler(Looper.myLooper()!!)

    private fun updateBackground(uri: String?) {
        if (!isVisible) return
        if (isDetached) return
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        Glide.with(requireActivity())
            .load(uri)
            .centerCrop()
            .error(ContextCompat.getDrawable(requireActivity(), R.drawable.default_background))
            .into<SimpleTarget<Drawable>>(
                object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                    }
                })

        mBackgroundTimer?.cancel()
    }

    override fun onDestroyView() {
        progressManager.disableProgressBar()
        progressManager.setRootView(null)
        super.onDestroyView()
    }

    private inner class UpdateBackgroundTask : TimerTask() {
        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

}