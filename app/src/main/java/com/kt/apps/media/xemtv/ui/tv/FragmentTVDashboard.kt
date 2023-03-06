package com.kt.apps.media.xemtv.ui.tv

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.leanback.app.ProgressBarManager
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.details.DetailsActivity
import com.kt.apps.media.xemtv.ui.main.CardPresenter
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import java.util.*
import javax.inject.Inject

class FragmentTVDashboard : BaseRowSupportFragment() {
    private val progressBarManager by lazy {
        ProgressBarManager()
    }

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
                    progressBarManager.hide()
                    val channelWithCategory = it.data.groupBy {
                        it.tvGroup
                    }
                    val cardPresenter = CardPresenter()
                    for ((group, channelList) in channelWithCategory) {
                        val headerItem = HeaderItem(group)
                        val adapter = ArrayObjectAdapter(cardPresenter)
                        for (channel in channelList) {
                            adapter.add(channel)
                        }
                        mRowsAdapter.add(ListRow(headerItem, adapter))
                    }
                    mainFragmentAdapter.fragmentHost.notifyDataReady(mainFragmentAdapter)
                }
                is DataState.Loading -> {
                    progressBarManager.show()
                }
                is DataState.Error -> {
                    progressBarManager.hide()
                }
                else -> {
                    progressBarManager.hide()
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
                progressBarManager.show()
            }
            is DataState.Success -> {
                progressBarManager.hide()
                val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                intent.putExtra(DetailsActivity.TV_CHANNEL, it.data)
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
                progressBarManager.hide()
                showErrorDialog(content = it.throwable.message)
            }
            else -> {
            }
        }
    }

    override fun onPause() {
//        mBackgroundManager.release()
        super.onPause()
    }
    override fun onStop() {
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
//                        mBackgroundManager.drawable = drawable
                    }
                })

        mBackgroundTimer?.cancel()
    }

    private inner class UpdateBackgroundTask : TimerTask() {
        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

}