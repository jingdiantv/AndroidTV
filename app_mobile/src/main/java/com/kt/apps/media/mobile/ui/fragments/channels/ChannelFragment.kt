package com.kt.apps.media.mobile.ui.fragments.channels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.ContactsContract.Data
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnScrollChangeListener
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import cn.pedant.SweetAlert.ProgressHelper
import com.google.android.material.navigation.NavigationBarView
import com.google.gson.Gson
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.adapter.OnItemRecyclerViewCLickListener
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityMainBinding
import com.kt.apps.media.mobile.ui.complex.ComplexLayoutHandler
import com.kt.apps.media.mobile.ui.complex.LandscapeLayoutHandler
import com.kt.apps.media.mobile.ui.complex.PortraitLayoutHandler
import com.kt.apps.media.mobile.ui.main.TVChannelViewModel
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.ui.playback.ITVServiceAidlInterface
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChannelFragment : BaseFragment<ActivityMainBinding>() {
    override val layoutResId: Int
        get() = R.layout.activity_main
    override val screenName: String
        get() = "Channel screen"

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val isLandscape: Boolean
        get() = resources.getBoolean(R.bool.is_landscape)

    private val progressHelper by lazy {
        ProgressHelper(this.context)
    }

    //Views
    private val progressDialog by lazy {
        binding.progressDialog
    }
    private val swipeRefreshLayout by lazy {
        binding.swipeRefreshLayout
    }

    private val mainRecyclerView by lazy {
        binding.mainChannelRecyclerView
    }

    private val skeletonScreen by lazy {
        KunSkeleton.bind(mainRecyclerView)
            .adapter(adapter)
            .recyclerViewLayoutItem(R.layout.item_row_channel_skeleton, R.layout.item_channel_skeleton)
            .build()
    }

    private val navigationRailView: NavigationBarView by lazy {
        binding.navigationRailView as NavigationBarView
    }

    private val onItemSelected by lazy {
        NavigationBarView.OnItemSelectedListener {
            return@OnItemSelectedListener onChangeItem(it)
        }
    }

    private val _onScrollListener by lazy {
        object : OnScrollListener() {
            var lastDetectedTime = System.currentTimeMillis()
            var lastDetectedTV = System.currentTimeMillis()

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (adapter.listItem.isEmpty()) {
                    return
                }
                val item = (binding.mainChannelRecyclerView.layoutManager as LinearLayoutManager)
                    .findLastCompletelyVisibleItemPosition()
                if (item == RecyclerView.NO_POSITION) {
                    return
                }
                val itemTitle = adapter.listItem[item].first
                if (itemTitle == TVChannelGroup.VOV.value || itemTitle == TVChannelGroup.VOH.value) {
                    if (navigationRailView.selectedItemId != R.id.radio && System.currentTimeMillis() - lastDetectedTime > 300) {
                        navigationRailView.setOnItemSelectedListener(null)
                        lastDetectedTime = System.currentTimeMillis()
                        navigationRailView.selectedItemId = R.id.radio
                        navigationRailView.setOnItemSelectedListener(onItemSelected)
                    }
                } else {
                    if (navigationRailView.selectedItemId != R.id.tv && System.currentTimeMillis() - lastDetectedTV > 300) {
                        navigationRailView.setOnItemSelectedListener(null)
                        lastDetectedTV = System.currentTimeMillis()
                        navigationRailView.selectedItemId = R.id.tv
                        navigationRailView.setOnItemSelectedListener(onItemSelected)
                    }
                }
            }
        }
    }

    private val adapter by lazy {
        TVDashboardAdapter().apply {
            onChildItemClickListener = { item, _  ->
                tvChannelViewModel?.getLinkStreamForChannel(item)
            }
        }
    }

    private val playbackViewModel: PlaybackViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[PlaybackViewModel::class.java].apply {
                this.videoState.observe(this@ChannelFragment, playbackStateObserver)
            }
        }
    }

    private val tvChannelViewModel: TVChannelViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[TVChannelViewModel::class.java].apply {
                this.tvChannelLiveData.observe(this@ChannelFragment, listTVChannelObserver)
                this.tvWithLinkStreamLiveData.observe(this@ChannelFragment, tvChannelStreamObserver)
            }
        }
    }

    private val listTVChannelObserver: Observer<DataState<List<TVChannel>>> by lazy {
        Observer { dataState ->
            when(dataState) {
                is DataState.Success -> {
                    swipeRefreshLayout.isRefreshing = false

                    skeletonScreen.hide {
                        adapter.onRefresh(dataState.data.groupAndSort())
                        mainRecyclerView.addOnScrollListener(_onScrollListener)
                        scrollToPosition(0)
                    }
                }
                is DataState.Loading -> {
                    mainRecyclerView.clearOnScrollListeners()
                    skeletonScreen.run()
                }
                else -> { }
            }
        }
    }

    private val tvChannelStreamObserver: Observer<DataState<TVChannelLinkStream>> by lazy {
        Observer {dataState ->
            dataState.takeIf { it is DataState.Loading }.apply {
                progressDialog.fadeIn {
                    progressHelper.spin()
                }
            } ?: progressDialog.fadeOut {
                progressHelper.stopSpinning()
            }
        }
    }

    private val playbackStateObserver: Observer<PlaybackViewModel.State> by lazy {
        Observer { state ->
            if (!isLandscape) {
                return@Observer
            }
            when(state) {
                PlaybackViewModel.State.IDLE -> {
                    with(mainRecyclerView) {
                        setPadding(0,  0, 0, 0)
                    }
                }
                PlaybackViewModel.State.LOADING, PlaybackViewModel.State.PLAYING -> {
                    with(mainRecyclerView) {
                        setPadding(0,0,0,screenHeight / 3)
                        clipToPadding = false
                    }
                }
                else -> {}
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        tvChannelViewModel?.getListTVChannel(savedInstanceState == null)
        playbackViewModel

        with(binding.mainChannelRecyclerView) {
            adapter = this@ChannelFragment.adapter
            layoutManager = LinearLayoutManager(this@ChannelFragment.context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
            setHasFixedSize(true)
            setItemViewCacheSize(9)
            viewTreeObserver.addOnGlobalLayoutListener {
                this@ChannelFragment.adapter.spanCount =
                    3.coerceAtLeast((width / 170 / resources.displayMetrics.scaledDensity).toInt())
            }
        }
    }


    override fun initAction(savedInstanceState: Bundle?) {
        binding.swipeRefreshLayout.setOnRefreshListener {
            tvChannelViewModel?.getListTVChannel(true)
        }
        with(binding.mainChannelRecyclerView) {
            addOnScrollListener(_onScrollListener)
        }
        navigationRailView.setOnItemSelectedListener { item ->
            return@setOnItemSelectedListener onChangeItem(item)
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent()
        intent.component = ComponentName("com.kt.apps.media.mobile.xemtv", "com.kt.apps.media.mobile.services.TVService")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            activity?.applicationContext?.startForegroundService(intent)
        } else {
            activity?.applicationContext?.startService(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        mainRecyclerView.clearOnScrollListeners()
    }

    private fun scrollToPosition(index: Int) {
        mainRecyclerView.removeOnScrollListener(_onScrollListener)
        mainRecyclerView.smoothScrollToPosition(index)
        mainRecyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    mainRecyclerView.addOnScrollListener(_onScrollListener)
                    mainRecyclerView.removeOnScrollListener(this)
                }
            }
        })
    }

    private fun onChangeItem(item: MenuItem): Boolean {
        if (item.itemId == R.id.radio) {
            if (navigationRailView.selectedItemId == item.itemId) {
                return false
            }
            scrollToPosition(8)
            return true
        }
        if (item.itemId == R.id.tv) {
            if (navigationRailView.selectedItemId == item.itemId) {
                return false
            }
            scrollToPosition(0)
            return true
        }
        return false
    }

    private fun List<TVChannel>.groupAndSort(): List<Pair<String, List<TVChannel>>> {
        return this.groupBy { it.tvGroup }
            .toList()
            .sortedWith(Comparator { o1, o2 ->
                return@Comparator if (o2.first == TVChannelGroup.VOV.value || o2.first == TVChannelGroup.VOH.value)
                    if (o1.first ==TVChannelGroup.VOH.value)  0  else -1
                else 1
            })
    }
}

