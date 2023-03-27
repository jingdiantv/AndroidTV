package com.kt.apps.media.xemtv.ui.football

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.football.model.FootballMatchWithStreamLink
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

class FootballPlaybackFragment : BasePlaybackFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val footballViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[FootballViewModel::class.java]
    }
    override val numOfRowColumns: Int
        get() = 5
    private var observer: Observer<DataState<FootballMatchWithStreamLink>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        footballViewModel.getAllMatches()

        arguments?.getParcelable<FootballMatchWithStreamLink?>(PlaybackActivity.EXTRA_FOOTBALL_MATCH)
            ?.let {
                playVideo(
                    it.match.getMatchName(),
                    it.match.league,
                    it.linkStreams.map { streamWithReferer ->
                        LinkStream(
                            streamWithReferer.m3u8Link,
                            streamWithReferer.referer,
                            streamWithReferer.m3u8Link
                        )
                    },
                    isLive = it.match.isLiveMatch,
                    isHls = true
                )
            }
        observer = Observer { dataState ->
            Logger.e(this, message = dataState::class.java.name)
            when (dataState) {
                is DataState.Success -> {
                    progressBarManager.hide()
                    val data = dataState.data
                    val linkStreams = data.linkStreams.map { streamWithReferer ->
                        LinkStream(
                            streamWithReferer.m3u8Link,
                            streamWithReferer.referer,
                            streamWithReferer.m3u8Link
                        )
                    }
                    playVideo(
                        data.match.getMatchName(),
                        data.match.league,
                        linkStreams,
                        isLive = data.match.isLiveMatch,
                        isHls = true
                    )
                }

                is DataState.Loading -> {
                    progressBarManager.show()
                }

                is DataState.Error -> {
                    progressBarManager.hide()
                }

                else -> {

                }
            }
        }
        footballViewModel.footMatchDataState.observe(viewLifecycleOwner, observer!!)

        footballViewModel.listFootMatchDataState.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    setupRowAdapter(it.data.filter {
                        it.isLiveMatch
                    }.ifEmpty {
                        it.data.sortedBy {
                            it.kickOffTimeInSecond
                        }
                    }, TVChannelPresenterSelector(requireActivity()))
                    onItemClickedListener = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
                        footballViewModel.getLinkStreamFor(item as FootballMatch)
                    }
                }

                else -> {

                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDetach() {
        Logger.d(this, message = "onStop")
        super.onDetach()
    }

    override fun onStop() {
        super.onStop()
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        fun newInstance(footballMatchWithStreamLink: FootballMatchWithStreamLink) = FootballPlaybackFragment().apply {
            val args = Bundle()
            args.putParcelable(PlaybackActivity.EXTRA_PLAYBACK_TYPE, PlaybackActivity.Type.FOOTBALL as Parcelable)
            args.putParcelable(PlaybackActivity.EXTRA_FOOTBALL_MATCH, footballMatchWithStreamLink)
            this.arguments = args
        }
    }
}