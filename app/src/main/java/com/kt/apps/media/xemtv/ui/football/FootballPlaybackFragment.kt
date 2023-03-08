package com.kt.apps.media.xemtv.ui.football

import android.os.Bundle
import android.provider.ContactsContract.RawContacts.Data
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.football.model.FootballMatchWithStreamLink
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

class FootballPlaybackFragment : BasePlaybackFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val footballViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[FootballViewModel::class.java]
    }

    override fun onDpadCenter() {

    }

    override fun onDpadDown() {

    }

    override fun onDpadUp() {

    }

    override fun onDpadLeft() {

    }

    override fun onDpadRight() {

    }

    override fun onKeyCodeChannelUp() {

    }

    override fun onKeyCodeChannelDown() {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        footballViewModel.getAllMatches()
        activity?.intent?.extras?.getParcelable<FootballMatchWithStreamLink>(PlaybackActivity.EXTRA_FOOTBALL_MATCH)
            ?.let {
                playVideo(
                    it.match.getMatchName(),
                    it.match.league,
                    it.linkStreams[0].referer,
                    it.linkStreams.map {
                        it.m3u8Link
                    }
                )
            }

        footballViewModel.footMatchDataState.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    progressBarManager.hide()
                    it.data.let {
                        playVideo(
                            it.match.getMatchName(),
                            it.match.league,
                            it.linkStreams[0].referer,
                            it.linkStreams.map {
                                it.m3u8Link
                            }
                        )
                    }
                }

                is DataState.Loading -> {
                    progressBarManager.show()
                }

                is DataState.Error -> {
                    progressBarManager.hide()
                    showErrorDialog(content = it.throwable.message)
                }

                else -> {

                }
            }
        }

        footballViewModel.listFootMatchDataState.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {

                }

                else -> {

                }
            }
        }

    }
}