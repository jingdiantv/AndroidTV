package com.kt.apps.media.mobile.ui.fragments.channels

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.provider.ContactsContract.Data
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.get
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import cn.pedant.SweetAlert.ProgressHelper
import com.google.android.material.navigation.NavigationBarItemView
import com.google.android.material.navigation.NavigationBarMenu
import com.google.android.material.navigation.NavigationBarView
import com.google.gson.Gson
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
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
import com.kt.apps.media.mobile.databinding.ItemSectionBinding
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.IChannelElement
import com.kt.apps.media.mobile.ui.main.TVChannelViewModel
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.utils.debounce
import com.kt.apps.media.mobile.utils.fastSmoothScrollToPosition
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Dictionary
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

typealias ResultData = Pair<List<TVChannel>, ExtensionResult>
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

    private val defaultSection by lazy {
        arrayListOf<SectionItem>(
            SectionItemElement.MenuItem(
                displayTitle = "TV",
                id = R.id.tv,
                icon = resources.getDrawable(com.kt.apps.core.R.drawable.ic_tv)
            ),
            SectionItemElement.MenuItem(
                displayTitle = "Radio",
                id = R.id.radio,
                icon = resources.getDrawable(com.kt.apps.core.R.drawable.ic_radio)
            )
        )
    }

    private val addExtensionSection by lazy {
        SectionItemElement.MenuItem(
            displayTitle = "Thêm nguồn",
            id = R.id.add_extension,
            icon = resources.getDrawable(R.drawable.round_add_circle_outline_24)
        )
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

    private val sectionRecyclerView by lazy {
        binding.sectionRecyclerView
    }

    private val skeletonScreen by lazy {
        KunSkeleton.bind(mainRecyclerView)
            .adapter(adapter)
            .recyclerViewLayoutItem(R.layout.item_row_channel_skeleton, R.layout.item_channel_skeleton)
            .build()
    }

    private val _tvChannelData by lazy {
        MutableStateFlow<DataState<List<TVChannel>>>(DataState.None())
    }

    private val _extensionsChannelData by lazy {
        MutableStateFlow<DataState<ExtensionResult>>(DataState.None())
    }

    private val debounceOnScrollListener  by lazy {
        debounce<Unit>(300, viewLifecycleOwner.lifecycleScope) {
            if (adapter.listItem.isEmpty()) {
                return@debounce
            }
            val item = (binding.mainChannelRecyclerView.layoutManager as LinearLayoutManager)
                .findFirstVisibleItemPosition()
            if (item == RecyclerView.NO_POSITION) {
                return@debounce
            }
            val itemTitle = adapter.listItem[item].first
            fun performSelected(id: Int) {
                sectionAdapter.selectForId(id)
            }
            adapter.listItem[item].second.firstOrNull()?.let {
                (it as? ChannelElement.ExtensionChannelElement)?.model?.sourceFrom
            }?.let {
                _cacheMenuItem[it]
            }?.run {
                performSelected(this)
            } ?: kotlin.run {
                performSelected(if (itemTitle == TVChannelGroup.VOV.value || itemTitle == TVChannelGroup.VOH.value) {
                    R.id.radio
                } else {
                    R.id.tv
                })
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

    private val sectionAdapter by lazy {
        SectionAdapter(requireContext()).apply {
            onItemRecyclerViewCLickListener = { item, _ ->
                if (onChangeItem(item)) {
                    this.currentSelectedItem = item
                }
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

    private val listExtensionsObserver: Observer<DataState<ExtensionResult>> by lazy {
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
    private var _cacheMenuItem: MutableMap<String, Int> = mutableMapOf<String, Int>()
    private val extensionListObserver: Observer<List<ExtensionsConfig>> by lazy {
        Observer {
            reloadNavigationBar(it)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        tvChannelViewModel?.getListTVChannel(savedInstanceState == null)
        playbackViewModel
        extensionsViewModel

        with(binding.sectionRecyclerView) {
            layoutManager = LinearLayoutManager(context).apply {
                orientation = if (isLandscape) VERTICAL else HORIZONTAL
            }
        }

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

        sectionRecyclerView?.apply {
            adapter = sectionAdapter
        }
        sectionAdapter.onRefresh(defaultSection + arrayListOf(addExtensionSection), notifyDataSetChange = true)
    }


    override fun initAction(savedInstanceState: Bundle?) {
        binding.swipeRefreshLayout.setOnRefreshListener {
            tvChannelViewModel?.getListTVChannel(true)
        }
        with(binding.mainChannelRecyclerView) {
            addOnScrollListener(_onScrollListener)
        }

        lifecycleScope.launch {
            combine(flow = _tvChannelData, flow2 = _extensionsChannelData) { tvChannel, extensionChannel ->
                return@combine Pair(tvChannel, extensionChannel)
            }.collect {
                if (it.first is DataState.Success && it.second is DataState.Success) {
                    combineDataAndRefresh(Pair((it.first as DataState.Success<List<TVChannel>>).data, (it.second as DataState.Success<ExtensionResult>).data))
                } else {
                    mainRecyclerView.clearOnScrollListeners()
                    skeletonScreen.run()
                }
            }
        }
//        sectionAdapter.onRefresh()
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

        val groupedEx = data.second.map { entry ->
            groupAndSort(entry.value).map {pair ->
                Pair<String, List<IChannelElement>>(
                    "${pair.first} (${entry.key.sourceName})",
                    pair.second.map { exChannel -> ChannelElement.ExtensionChannelElement(exChannel) }
                )
            }
        }.flatten()
        swipeRefreshLayout.isRefreshing = false
        adapter.onRefresh(groupedTV + groupedEx)
        skeletonScreen.hide {
            mainRecyclerView.addOnScrollListener(_onScrollListener)
            scrollToPosition(0)
        }
    }

    private fun scrollToPosition(index: Int) {
        Log.d(TAG, "scrollToPosition: $index")
        mainRecyclerView.fastSmoothScrollToPosition(index)
    }


    private fun onChangeItem(item: SectionItem): Boolean {
        val currentSelected = sectionAdapter.currentSelectedItem
        if (item.id == currentSelected?.id) return false

        when(item.id) {
            R.id.radio -> scrollToPosition(8)
            R.id.tv -> scrollToPosition(0)
            R.id.add_extension -> {
                val dialog = AddExtensionFragment()
                dialog.onSuccess = {
                    it.dismiss()
                    onAddedExtension()
                }
                dialog.show(this@ChannelFragment.parentFragmentManager, AddExtensionFragment.TAG)
                return false
            }
            else -> {
                val title = item.displayTitle
                return (extensionsViewModel?.extensionsConfigs?.value ?: emptyList()).findLast {
                    it.sourceName == title
                }?.run {
                    val index = adapter.listItem.indexOfFirst {
                        val channel = it.second.firstOrNull()
                        (channel as? ChannelElement.ExtensionChannelElement)?.model?.sourceFrom?.equals(title) == true
                    }.takeIf {
                        it != -1
                    }?.run {
                        scrollToPosition(this)
                    }

                    true
                } ?: false
            }
        }
        return true
    }

    private fun onAddedExtension() {
        showSuccessDialog(
            content = "Thêm nguồn kênh thành công!\r\nKhởi động lại ứng dụng để kiểm tra nguồn kênh"
        )
    }

    private fun showAlertRemoveExtension(sourceName: String) {
        AlertDialog.Builder(context, R.style.AlertDialogTheme).apply {
            setMessage("Bạn có muốn xóa nguồn $sourceName?")
            setCancelable(true)
            setPositiveButton("Có") { dialog, which ->
                extensionsViewModel?.deleteExtension(sourceName = sourceName)
                dialog.dismiss()
            }
            setNegativeButton("Không") { dialog, _ ->
                dialog.dismiss()
            }
        }
            .create()
            .show()

    }

    private fun reloadNavigationBar(extra: List<ExtensionsConfig>) {
        _cacheMenuItem = mutableMapOf()

        val extraSection = extra.map {
            val id = View.generateViewId()
            _cacheMenuItem[it.sourceName] = id
            SectionItemElement.MenuItem(
                displayTitle = it.sourceName,
                id = id,
                icon = resources.getDrawable(R.drawable.round_add_circle_outline_24)
            )
        }
        sectionAdapter.onRefresh(
            defaultSection + extraSection + arrayListOf(addExtensionSection),
            notifyDataSetChange = true
        )
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
