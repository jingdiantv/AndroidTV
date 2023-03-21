package com.kt.apps.media.xemtv.ui.playback

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.PresenterSelector
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.PlaybackException
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.main.MainActivity
import dagger.android.AndroidInjector
import javax.inject.Inject
import kotlin.math.max

/** Handles video playback with media controls. */
class TVPlaybackVideoFragment : BasePlaybackFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val tvChannelViewModel: TVChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[TVChannelViewModel::class.java]
    }

    override fun onHandlePlayerError(error: PlaybackException) {
        super.onHandlePlayerError(error)
        tvChannelViewModel.retryGetLastWatchedChannel()
    }

    private var mCurrentSelectedChannel: TVChannel? = null
    private var mSelectedPosition: Int = 0
    private var mPlayingPosition: Int = 0
    override val numOfRowColumns: Int
        get() = 4

    private fun setupRowAdapter(tvChannelList: List<TVChannel>) {
        mSelectedPosition = mCurrentSelectedChannel?.let {
            val lastChannel = tvChannelList.findLast {
                it.channelId == mCurrentSelectedChannel!!.channelId
            }
            tvChannelList.lastIndexOf(lastChannel)
        } ?: 0
        mPlayingPosition = mSelectedPosition
        Logger.d(this, message = "setupRowAdapter: $mSelectedPosition")
        val cardPresenterSelector: PresenterSelector = TVChannelPresenterSelector(requireActivity())
        val mAdapter = ArrayObjectAdapter(cardPresenterSelector)
        mAdapter.addAll(0, tvChannelList)
        this.mAdapter = mAdapter
        updateAdapter()
    }

    override fun setSelectedPosition(position: Int) {
        mSelectedPosition = position
        mGridViewHolder?.gridView?.setSelectedPositionSmooth(position)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (tvChannelViewModel.tvChannelLiveData.value !is DataState.Success) {
            tvChannelViewModel.getListTVChannel(false)
        }

        val tvChannel = arguments?.getParcelable<TVChannelLinkStream?>(PlaybackActivity.EXTRA_TV_CHANNEL)
        tvChannelViewModel.markLastWatchedChannel(tvChannel)
        tvChannel?.let {
            mCurrentSelectedChannel = it.channel
            if (it.channel.isRadio) {
                getBackgroundView()?.setBackgroundResource(R.drawable.bg_radio_playing)
            }
            playVideo(
                tvChannel.channel.tvChannelName,
                tvChannel.channel.tvGroup,
                tvChannel.channel.tvChannelWebDetailPage,
                tvChannel.linkStream,
                true
            )
        } ?: let {
        }
        onItemClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
            (mAdapter as ArrayObjectAdapter).indexOf(item)
                .takeIf {
                    it > -1
                }?.let {
                    mSelectedPosition = it
                }
        }
        tvChannelViewModel.tvWithLinkStreamLiveData.observe(viewLifecycleOwner) {
            streamingByDataState(it)
        }

        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            loadChannelListByDataState(it)
        }
    }

    private fun loadChannelListByDataState(dataState: DataState<List<TVChannel>>) {
        when (dataState) {
            is DataState.Success -> {
                setupRowAdapter(dataState.data)
                if (mPlayingPosition <= 0 && mCurrentSelectedChannel != null) {
                    mPlayingPosition = dataState.data.indexOfLast {
                        it.channelId == mCurrentSelectedChannel!!.channelId
                    }.takeIf {
                        it >= 0
                    } ?: 0
                }
            }
            is DataState.Error -> {

            }
            is DataState.Loading -> {

            }
            else -> {

            }
        }
    }

    private fun streamingByDataState(dataState: DataState<TVChannelLinkStream>?) {
        when (dataState) {
            is DataState.Success -> {
                mCurrentSelectedChannel = dataState.data.channel
                progressBarManager.hide()
                val tvChannel = dataState.data
                if (tvChannel.channel.isRadio) {
                    getBackgroundView()?.setBackgroundResource(R.drawable.bg_radio_playing)
                }
                playVideo(
                    tvChannel.channel.tvChannelName,
                    tvChannel.channel.tvGroup,
                    tvChannel.channel.tvChannelWebDetailPage,
                    tvChannel.linkStream,
                    true
                )
                Logger.d(this, message = "Play media source")
            }

            is DataState.Loading -> {
                progressBarManager.show()
            }

            is DataState.Error -> {
                progressBarManager.hide()

                showErrorDialog(content = dataState.throwable.message,
                    onSuccessListener = {
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    })
            }

            else -> {
                progressBarManager.hide()
            }
        }
    }

    override fun onDetach() {
        progressBarManager.hide()
        Logger.d(this, message = "onDetach")
        super.onDetach()
    }

    override fun onKeyCodeChannelDown() {
        super.onKeyCodeChannelDown()
        mPlayingPosition = max(0, mPlayingPosition) - 1
        setSelectedPosition(mPlayingPosition)
        Logger.d(this, message = "onKeyCodeChannelDown: $mPlayingPosition")
        if (mPlayingPosition > 0) {
            val item = mAdapter?.get(mPlayingPosition)
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
        }
    }

    override fun onKeyCodeChannelUp() {
        mPlayingPosition = max(0, mPlayingPosition) + 1

        setSelectedPosition(mPlayingPosition)
        Logger.d(this, message = "onKeyCodeChannelUp: $mPlayingPosition")
        val maxItemCount = mGridViewHolder?.gridView?.adapter?.itemCount ?: 0
        if (mPlayingPosition <= maxItemCount - 1) {
            val item = mAdapter?.get(mPlayingPosition)
            tvChannelViewModel.getLinkStreamForChannel(item as TVChannel)
        }
    }

    fun hideChannelMenu() {
        hideOverlay()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return injector
    }

    companion object {
        fun newInstance(type: PlaybackActivity.Type, tvChannelLinkStream: TVChannelLinkStream) = TVPlaybackVideoFragment().apply {
            val args = Bundle()
            args.putParcelable(PlaybackActivity.EXTRA_PLAYBACK_TYPE, type)
            args.putParcelable(PlaybackActivity.EXTRA_TV_CHANNEL, tvChannelLinkStream)
            this.arguments = args
        }
    }
}