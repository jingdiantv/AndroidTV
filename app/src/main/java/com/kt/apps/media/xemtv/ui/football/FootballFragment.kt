package com.kt.apps.media.xemtv.ui.football

import android.content.Intent
import android.os.Parcelable
import android.view.View
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseGridViewFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.databinding.FragmentFootballBinding
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import javax.inject.Inject

class FootballFragment : BaseGridViewFragment<FragmentFootballBinding>() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private val footballViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[FootballViewModel::class.java]
    }

    override val layoutRes: Int
        get() = R.layout.fragment_football

    override fun onCreatePresenter(): VerticalGridPresenter {
        return VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM)
            .apply {
                this.numberOfColumns = 5
            }
    }

    override fun onItemViewSelectedListener(): OnItemViewSelectedListener {
        return OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->

        }
    }

    override fun onItemViewClickedListener(): OnItemViewClickedListener {
        return OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            if (item is FootballMatch) {
                footballViewModel.getLinkStreamFor(item)
            }
        }
    }

    override fun onCreateAdapter() {
        mAdapter = ArrayObjectAdapter(TVChannelPresenterSelector(requireActivity()))
        updateAdapter()
    }

    override fun initView(rootView: View) {
    }

    override fun initAction(rootView: View) {
        footballViewModel.getAllMatches()
        footballViewModel.listFootMatchDataState.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    progressManager.hide()
                    val data = it.data
                    (mAdapter as ArrayObjectAdapter).apply {
                        clear()
                        addAll(0, data)
                    }
                }

                is DataState.Loading -> {
                    progressManager.show()
                }

                else -> {

                }
            }
        }
        footballViewModel.footMatchDataState.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    progressManager.hide()
                    val data = it.data
                    startActivity(
                        Intent(requireActivity(), PlaybackActivity::class.java).apply {
                            putExtra(PlaybackActivity.EXTRA_PLAYBACK_TYPE, PlaybackActivity.Type.FOOTBALL as Parcelable)
                            putExtra(PlaybackActivity.EXTRA_FOOTBALL_MATCH, data)
                        }
                    )
                }

                is DataState.Loading -> {
                    progressManager.show()
                }

                is DataState.Error -> {
                    progressManager.hide()
                    showErrorDialog(content = it.throwable.message)
                }

                else -> {
                }
            }
        }
    }

}