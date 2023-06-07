package com.kt.apps.media.xemtv.ui.tv

import android.view.View
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.kt.apps.core.base.BaseGridViewFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.databinding.DefaultGridFragmentBinding
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import javax.inject.Inject

class FragmentTVGrid : BaseGridViewFragment<DefaultGridFragmentBinding>() {
    override val layoutRes: Int
        get() = com.kt.apps.core.R.layout.default_grid_fragment


    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val filterGroup by lazy {
        requireArguments().getString("filterGroup")
    }

    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }

    override fun onCreatePresenter(): VerticalGridPresenter {
        return VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM).apply {
            this.numberOfColumns = 5
        }
    }

    override fun onItemViewSelectedListener(): OnItemViewSelectedListener {
        return OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
        }
    }

    override fun onItemViewClickedListener(): OnItemViewClickedListener {
        return OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->

        }
    }

    override fun onCreateAdapter() {
        val cardPresenter = TVChannelPresenterSelector(requireActivity())
        mAdapter = ArrayObjectAdapter(cardPresenter)
        updateAdapter()
    }

    override fun initView(rootView: View) {

    }

    override fun initAction(rootView: View) {
        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Success -> {
                    (mAdapter as ArrayObjectAdapter)
                        .addAll(0, it.data)
                }

                else -> {

                }
            }
        }
    }
}