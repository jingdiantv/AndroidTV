package com.kt.apps.media.xemtv.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.leanback.widget.OnItemViewClickedListener
import com.google.android.exoplayer2.PlaybackException
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector

class FragmentExtensionsPlayback : BasePlaybackFragment() {
    override val numOfRowColumns: Int
        get() = 5

    private var itemToPlay: TVChannel? = null
    private val listCurrentItem by lazy {
        mutableListOf<TVChannel>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemToPlay = requireArguments()[EXTRA_TV_CHANNEL] as TVChannel?
    }

    override fun onHandlePlayerError(error: PlaybackException) {
        super.onHandlePlayerError(error)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemToPlay?.let {
            playVideo(
                TVChannelLinkStream(
                    it,
                    listOf(it.tvChannelWebDetailPage)
                )
            )
        }

        requireArguments()[EXTRA_TV_CHANNEL_LIST]?.let {
            listCurrentItem.addAll(it as Array<TVChannel>)

            setupRowAdapter(listCurrentItem, TVChannelPresenterSelector(requireActivity()))
            onItemClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
                itemToPlay = item as TVChannel
                playVideo(
                    TVChannelLinkStream(
                        itemToPlay!!,
                        listOf(itemToPlay!!.tvChannelWebDetailPage)
                    )
                )
            }
        }


    }

    private fun playVideo(tvChannel: TVChannelLinkStream) {
        playVideo(
            tvChannel.channel.tvChannelName,
            null,
            tvChannel.channel.tvChannelWebDetailPage,
            tvChannel.linkStream,
            true,
            isHls = tvChannel.channel.isHls
        )

    }

    companion object {
        private const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        private const val EXTRA_TV_CHANNEL_LIST = "extra:tv_channel_list"
        fun newInstance(
            tvChannel: TVChannel,
            channelList: List<TVChannel>
        ) = FragmentExtensionsPlayback().apply {
            arguments = bundleOf(
                EXTRA_TV_CHANNEL to tvChannel,
                EXTRA_TV_CHANNEL_LIST to channelList.toTypedArray()
            )
        }
    }
}