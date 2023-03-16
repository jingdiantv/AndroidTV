package com.kt.apps.media.xemtv.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kt.apps.core.base.DataState
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.BrowseErrorActivity
import com.kt.apps.media.xemtv.ui.details.DetailsActivity
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.presenter.CardPresenter
import com.kt.apps.media.xemtv.ui.TVChannelViewModel
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import java.util.*
import javax.inject.Inject

/**
 * Loads a grid of cards with tc channel to browse.
 */
class MainFragment : BrowseSupportFragment(), HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val tvChannelViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[TVChannelViewModel::class.java]
    }

    private val mHandler = Handler(Looper.myLooper()!!)
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        prepareBackgroundManager()

        setupUIElements()

        setupEventListeners()

        tvChannelViewModel.tvChannelLiveData.observe(viewLifecycleOwner) {
            handleListTVChannel(it)
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

    private fun handleListTVChannel(dataState: DataState<List<TVChannel>>) {
        when (dataState) {
            is DataState.Success -> {
                progressBarManager.hide()
                loadRows(dataState.data)
            }

            is DataState.Loading -> {
                progressBarManager.show()
            }

            else -> {
                progressBarManager.hide()
            }
        }
    }

    private fun loadRows(tvChannelList: List<TVChannel>) {

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()
        val map = tvChannelList.groupBy {
            it.tvGroup
        }
        var count = 0
        for ((key, listChannel) in map) {
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            listChannel.forEach {
                listRowAdapter.add(it)
            }

            val header = HeaderItem(count.toLong(), key)
            rowsAdapter.add(ListRow(header, listRowAdapter))
            count++
        }

        val gridHeader = HeaderItem(count.toLong(), "Cài đặt")

        val mGridPresenter = GridItemPresenter()
        val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
        gridRowAdapter.add(resources.getString(R.string.grid_view))
        gridRowAdapter.add(getString(R.string.error_fragment))
        gridRowAdapter.add(resources.getString(R.string.personal_settings))
        rowsAdapter.add(ListRow(gridHeader, gridRowAdapter))

        adapter = rowsAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
        mBackgroundTimer?.cancel()
    }

    private fun prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
        mDefaultBackground = ContextCompat.getDrawable(requireActivity(), R.drawable.default_background)
        mMetrics = resources.displayMetrics
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireActivity(), com.kt.skeleton.R.color.skeleton_elements)
        searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.search_opaque)
    }


    private fun setupEventListeners() {
        setOnSearchClickedListener {
            Toast.makeText(requireActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
                .show()
        }

        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private var selectedView: ImageCardView? = null

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {

            if (item is TVChannel) {
                Log.d(TAG, "Item: $item")
                tvChannelViewModel.getLinkStreamForChannel(tvDetail = item)
                selectedView = itemViewHolder.view as ImageCardView
                itemViewHolder.view
            } else if (item is String) {
                if (item.contains(getString(R.string.error_fragment))) {
                    val intent = Intent(requireActivity(), BrowseErrorActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(requireActivity(), item, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            if (item is TVChannel) {
                mBackgroundUri = item.logoChannel
                startBackgroundTimer()
            }
        }
    }

    private fun updateBackground(uri: String?) {
        Glide.with(requireActivity())
            .load(uri)
            .centerCrop()
            .error(mDefaultBackground)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    mBackgroundManager.drawable = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    mBackgroundManager.drawable = placeholder
                }

            })
        mBackgroundTimer?.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    private inner class UpdateBackgroundTask : TimerTask() {

        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

    private inner class GridItemPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.default_background))
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            (viewHolder.view as TextView).text = item as String
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
    }

    companion object {
        private const val TAG = "MainFragment"

        private const val BACKGROUND_UPDATE_DELAY = 300
        private const val GRID_ITEM_WIDTH = 200
        private const val GRID_ITEM_HEIGHT = 200
        private const val NUM_ROWS = 6
        private const val NUM_COLS = 15
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }
}