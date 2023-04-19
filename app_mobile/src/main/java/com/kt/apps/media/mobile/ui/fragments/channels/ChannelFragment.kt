package com.kt.apps.media.mobile.ui.fragments.channels

import android.os.Bundle
import android.provider.ContactsContract.Data
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import cn.pedant.SweetAlert.ProgressHelper
import com.google.android.material.navigation.NavigationBarView
import com.google.gson.Gson
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityMainBinding
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.ui.main.TVChannelViewModel
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.utils.debounce
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

typealias ResultData = Pair<List<TVChannel>, List<ExtensionsChannel>>
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

    private val _tvChannelData by lazy {
        MutableStateFlow<DataState<List<TVChannel>>>(DataState.None())
    }

    private val _extensionsChannelData by lazy {
        MutableStateFlow<DataState<List<ExtensionsChannel>>>(DataState.None())
    }

    private val onItemSelected by lazy {
        NavigationBarView.OnItemSelectedListener {
            return@OnItemSelectedListener onChangeItem(it)
        }
    }

    private val debounceOnScrollListener  by lazy {
        debounce<Unit>(300, viewLifecycleOwner.lifecycleScope) {
            if (adapter.listItem.isEmpty()) {
                return@debounce
            }
            val item = (binding.mainChannelRecyclerView.layoutManager as LinearLayoutManager)
                .findLastCompletelyVisibleItemPosition()
            if (item == RecyclerView.NO_POSITION) {
                return@debounce
            }
            val itemTitle = adapter.listItem[item].first
            if (itemTitle == TVChannelGroup.VOV.value || itemTitle == TVChannelGroup.VOH.value) {
                if (navigationRailView.selectedItemId != R.id.radio) {
                    navigationRailView.setOnItemSelectedListener(null)
                    navigationRailView.selectedItemId = R.id.radio
                    navigationRailView.setOnItemSelectedListener(onItemSelected)
                }
            } else {
                if (navigationRailView.selectedItemId != R.id.tv) {
                    navigationRailView.setOnItemSelectedListener(null)
                    navigationRailView.selectedItemId = R.id.tv
                    navigationRailView.setOnItemSelectedListener(onItemSelected)
                }
            }
        }
    }

    private val _onScrollListener by lazy {
        object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                debounceOnScrollListener(Unit)
            }
        }
    }

    private val adapter by lazy {
        TVDashboardAdapter().apply {
            onChildItemClickListener = { item, _  ->
                when(item) {
                    is ChannelElement.TVChannelElement -> tvChannelViewModel?.getLinkStreamForChannel(item.model)
                    is ChannelElement.ExtensionChannelElement -> tvChannelViewModel?.getExtensionChannel(item.model)
                }
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

    private val extensionsViewModel: ExtensionsViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[ExtensionsViewModel::class.java].apply {
                this.extensionsConfigs.observe(this@ChannelFragment, extensionListObserver)
                this.extensionsChannelData.observe(this@ChannelFragment, listExtensionsObserver)
            }
        }
    }

    private val listTVChannelObserver: Observer<DataState<List<TVChannel>>> by lazy {
        Observer { dataState ->
            _tvChannelData.tryEmit(dataState)
        }
    }

    private val listExtensionsObserver: Observer<DataState<List<ExtensionsChannel>>> by lazy {
        Observer { dataState ->
            _extensionsChannelData.tryEmit(dataState)
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

    private val extensionListObserver: Observer<List<ExtensionsConfig>> by lazy {
        Observer {
            reloadNavigationBar(it)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        tvChannelViewModel?.getListTVChannel(savedInstanceState == null)
        playbackViewModel
        extensionsViewModel

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
        lifecycleScope.launch {
            combine(flow = _tvChannelData, flow2 = _extensionsChannelData) { tvChannel, extensionChannel ->
                return@combine Pair(tvChannel, extensionChannel)
            }.collect {
                if (it.first is DataState.Success && it.second is DataState.Success) {
                    combineDataAndRefresh(Pair((it.first as DataState.Success<List<TVChannel>>).data, (it.second as DataState.Success<List<ExtensionsChannel>>).data))
                } else {
                    mainRecyclerView.clearOnScrollListeners()
                    skeletonScreen.run()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mainRecyclerView.clearOnScrollListeners()
    }

    private fun combineDataAndRefresh(data: ResultData) {
        val groupedTV = groupAndSort(data.first).map {
            return@map Pair<String, List<IChannelElement>>(it.first, it.second.map {tvChannel ->
                ChannelElement.TVChannelElement(tvChannel)
            })
        }
        val groupedEx = groupAndSort(data.second).map {
            return@map Pair<String, List<IChannelElement>>(it.first, it.second.map {exChannel ->
                ChannelElement.ExtensionChannelElement(exChannel)
            })
        }
        swipeRefreshLayout.isRefreshing = false
        adapter.onRefresh(groupedTV + groupedEx)
        skeletonScreen.hide {
            mainRecyclerView.addOnScrollListener(_onScrollListener)
            scrollToPosition(0)
        }
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

        if  (item.itemId == R.id.add_extension) {
            val dialog = AddExtensionFragment()
            dialog.onSuccess = {
                it.dismiss()
                onAddedExtension()
            }
            dialog.show(this@ChannelFragment.parentFragmentManager, AddExtensionFragment.TAG)
        }
        return false
    }

    private fun onAddedExtension() {
        showSuccessDialog(
            content = "Thêm nguồn kênh thành công!\r\nKhởi động lại ứng dụng để kiểm tra nguồn kênh"
        )

    }

    private fun reloadNavigationBar(extra: List<ExtensionsConfig>) {
        val menu = navigationRailView.menu.let {
            val defaults = arrayListOf(it.findItem(R.id.tv), it.findItem(R.id.radio))
            val addSourceItem = it.findItem(R.id.add_extension)
            it.clear()
            defaults.forEach {item ->
                it.add(item.groupId, item.itemId, item.order, item.title).icon = item.icon
            }

            extra.forEach {item ->
                it.add(item.sourceName)
                    .setIcon(R.drawable.round_add_circle_outline_24)
            }

            it.add(addSourceItem.groupId, addSourceItem.itemId, Menu.NONE, addSourceItem.title)
                .icon = addSourceItem.icon
            return@let it
        }
        navigationRailView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
    }

    private inline fun <reified T> groupAndSort(list: List<T>) : List<Pair<String, List<T>>> {
        return when(T::class) {
            TVChannel::class -> list.groupBy { (it as TVChannel).tvGroup }
                .toList()
                .sortedWith(Comparator { o1, o2 ->
                    return@Comparator if (o2.first == TVChannelGroup.VOV.value || o2.first == TVChannelGroup.VOH.value)
                        if (o1.first ==TVChannelGroup.VOH.value)  0  else -1
                    else 1
                })
            ExtensionsChannel::class -> list.groupBy { (it as ExtensionsChannel).tvGroup }
                .toList()
                .sortedWith(Comparator { o1, o2 ->
                    return@Comparator o1.first.compareTo(o2.first)
                })
            else -> emptyList()
        }
    }
}

