package com.kt.apps.media.mobile.ui.fragments.channels

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.kt.apps.core.base.BaseFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.showSuccessDialog
import com.kt.apps.media.mobile.BuildConfig
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityMainBinding
import com.kt.apps.media.mobile.models.NetworkState
import com.kt.apps.media.mobile.ui.fragments.dialog.AddExtensionFragment
import com.kt.apps.media.mobile.ui.fragments.models.ExtensionsViewModel
import com.kt.apps.media.mobile.ui.fragments.models.NetworkStateViewModel
import com.kt.apps.media.mobile.ui.fragments.models.TVChannelViewModel
import com.kt.apps.media.mobile.ui.main.ChannelElement
import com.kt.apps.media.mobile.ui.main.TVDashboardAdapter
import com.kt.apps.media.mobile.utils.debounce
import com.kt.apps.media.mobile.utils.fastSmoothScrollToPosition
import com.kt.apps.media.mobile.utils.groupAndSort
import com.kt.apps.media.mobile.utils.screenHeight
import com.kt.skeleton.KunSkeleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
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
        if (BuildConfig.isBeta)
            arrayListOf(
                SectionItemElement.MenuItem(
                    displayTitle = "Thêm nguồn",
                    id = R.id.add_extension,
                    icon = resources.getDrawable(R.drawable.round_add_circle_outline_24)
                )
            )
        else emptyList()
    }

    //Views
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
            .itemCount(10)
            .recyclerViewLayoutItem(
                R.layout.item_row_channel_skeleton,
                R.layout.item_channel_skeleton
            )
            .build()
    }

    private val _tvChannelData by lazy {
        MutableStateFlow<List<TVChannel>>(emptyList())
    }

    private val debounceOnScrollListener by lazy {
        debounce<Unit>(300, viewLifecycleOwner.lifecycleScope) {
            if (adapter.listItem.isEmpty()) {
                return@debounce
            }
            (mainRecyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                .let { if (it == NO_POSITION) (mainRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() else it }
                .takeIf { it != NO_POSITION }
                ?.run {
                    Log.d(TAG, "debounceOnScrollListener: $this")
                    val id = findMenuIdByItemPosition(this)
                    val adapterId = sectionAdapter.selectForId(id)
                    sectionRecyclerView.fastSmoothScrollToPosition(adapterId)
                }
        }
    }

    private fun findMenuIdByItemPosition(position: Int): Int {
        return adapter.listItem[position].second.firstOrNull()?.let {
            (it as? ChannelElement.ExtensionChannelElement)?.model?.sourceFrom
        }?.let { _cacheMenuItem[it] } ?: kotlin.run {
            val itemTitle = adapter.listItem[position].first
            if (itemTitle == TVChannelGroup.VOV.value || itemTitle == TVChannelGroup.VOH.value) {
                R.id.radio
            } else {
                R.id.tv
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
            onItemLongCLickListener = { item ->
                showAlertRemoveExtension(item.displayTitle)
                true
            }
        }
    }

    private val adapter by lazy {
        TVDashboardAdapter().apply {
            onChildItemClickListener = { item, _ ->
                when (item) {
                    is ChannelElement.TVChannelElement -> onClickItemChannel(item.model)
                    is ChannelElement.ExtensionChannelElement -> tvChannelViewModel?.getExtensionChannel(
                        item.model
                    )
                }
            }
        }
    }

    private val playbackViewModel: PlaybackViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[PlaybackViewModel::class.java]
        }
    }

    private val tvChannelViewModel: TVChannelViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
        }
    }

    private val networkStateViewModel: NetworkStateViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[NetworkStateViewModel::class.java]
        }
    }

    private val extensionsViewModel: ExtensionsViewModel? by lazy {
        activity?.run {
            ViewModelProvider(this, factory)[ExtensionsViewModel::class.java]
        }
    }

    private val listTVChannelObserver: Observer<DataState<List<TVChannel>>> by lazy {
        Observer { dataState ->
            when (dataState) {
                is DataState.Success -> _tvChannelData.value = dataState.data
                is DataState.Error -> swipeRefreshLayout.isRefreshing = false
                else -> {}
            }
        }
    }

    private var _cacheMenuItem: MutableMap<String, Int> = mutableMapOf<String, Int>()
    override fun initView(savedInstanceState: Bundle?) {

        with(binding.mainChannelRecyclerView) {
            adapter = this@ChannelFragment.adapter
            layoutManager = LinearLayoutManager(this@ChannelFragment.context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 9
            }
            setHasFixedSize(true)
            setItemViewCacheSize(9)
            doOnPreDraw {
                val spanCount = 3.coerceAtLeast((mainRecyclerView.measuredWidth / 220.dpToPx()))
                this@ChannelFragment.adapter.spanCount = spanCount
            }
        }

        sectionRecyclerView.apply {
            adapter = this@ChannelFragment.sectionAdapter
            layoutManager = LinearLayoutManager(context).apply {
                orientation = if (isLandscape) VERTICAL else HORIZONTAL
            }
        }
        sectionAdapter.onRefresh(defaultSection + addExtensionSection, notifyDataSetChange = true)
        _tvChannelData.value = emptyList()

        skeletonScreen.run()
    }


    override fun initAction(savedInstanceState: Bundle?) {
        tvChannelViewModel?.apply {
            tvChannelLiveData.observe(this@ChannelFragment, listTVChannelObserver)
        }
        playbackViewModel
        extensionsViewModel?.loadExtensionData()

        with(binding.swipeRefreshLayout) {
            setDistanceToTriggerSync(screenHeight / 3)
            setOnRefreshListener {
                _tvChannelData.value = emptyList()
                skeletonScreen.run()
                tvChannelViewModel?.getListTVChannel(true)
            }
        }
        with(binding.mainChannelRecyclerView) {
            addOnScrollListener(_onScrollListener)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    _tvChannelData.collectLatest { tvChannel ->
                        delay(500)
                        if (tvChannel.isNotEmpty())
                            reloadOriginalSource(tvChannel)
                    }
                }

                if (isLandscape)
                    launch {
                        playbackViewModel?.state?.collectLatest {state ->
                            with(mainRecyclerView) {
                                when (state) {
                                    PlaybackViewModel.State.IDLE -> setPadding(0, 0, 0, 0)
                                    PlaybackViewModel.State.LOADING, PlaybackViewModel.State.LOADING -> {
                                        setPadding(0,0,0,(screenHeight * 0.4).toInt())
                                        clipToPadding = false
                                    }
                                    else -> { }
                                }
                            }
                        }
                    }

                launch {
                    extensionsViewModel?.perExtensionChannelData?.collect {
                        appendExtensionSource(it)
                    }
                }

                launch {
                    extensionsViewModel?.extensionsConfigs?.collectLatest {
                        reloadNavigationBar(it)
                    }
                }

                launch {
                    networkStateViewModel?.networkStatus?.collectLatest {
                        if (it == NetworkState.Connected && adapter.itemCount == 0)
                            tvChannelViewModel?.getListTVChannel(forceRefresh = true)
                    }
                }
            }
        }

        tvChannelViewModel?.getListTVChannel(savedInstanceState != null)
    }

    override fun onStop() {
        super.onStop()
        mainRecyclerView.clearOnScrollListeners()
    }

    override fun onStart() {
        super.onStart()
        mainRecyclerView.addOnScrollListener(_onScrollListener)
    }
    private fun reloadOriginalSource(data: List<TVChannel>) {
        val grouped = groupAndSort(data).map {
            Pair(
                it.first,
                it.second.map { tvChannel -> ChannelElement.TVChannelElement(tvChannel) })
        }
        swipeRefreshLayout.isRefreshing = false
        adapter.onRefresh(grouped)
        extensionsViewModel?.perExtensionChannelData?.replayCache?.forEach {
            appendExtensionSource(it)
        }
        sectionAdapter.onRefresh(defaultSection + addExtensionSection, notifyDataSetChange = true)
        sectionRecyclerView.fastSmoothScrollToPosition(0)
        skeletonScreen.hide {
            scrollToPosition(0)
        }
    }

    private fun appendExtensionSource(data: Map<ExtensionsConfig, List<ExtensionsChannel>>) {
        data.forEach { entry ->
            val grouped = groupAndSort(entry.value).map {
                Pair(
                    "${it.first} (${entry.key.sourceName})",
                    it.second.map { exChannel -> ChannelElement.ExtensionChannelElement(exChannel) }
                )
            }
            adapter.onAdd(grouped)
        }
    }

    private fun scrollToPosition(index: Int) {
        Log.d(TAG, "scrollToPosition: $index")
        mainRecyclerView.fastSmoothScrollToPosition(index)
    }


    private fun onChangeItem(item: SectionItem): Boolean {

        when (item.id) {
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
                        (channel as? ChannelElement.ExtensionChannelElement)?.model?.sourceFrom?.equals(
                            title
                        ) == true
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
                deleteExtension(sourceName = sourceName)
                dialog.dismiss()
            }
            setNegativeButton("Không") { dialog, _ ->
                dialog.dismiss()
            }
        }
            .create()
            .show()

    }

    private fun deleteExtension(sourceName: String) {
        extensionsViewModel?.deleteExtension(sourceName = sourceName)
        adapter.listItem.filter {
            return@filter (it.second.firstOrNull() as? ChannelElement.ExtensionChannelElement)
                ?.model
                ?.sourceFrom == sourceName
        }.forEach {
            adapter.onDelete(it)
        }
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
            defaultSection + extraSection + addExtensionSection,
            notifyDataSetChange = true
        )
    }

    private fun onClickItemChannel(channel: TVChannel) {
        tvChannelViewModel?.loadLinkStreamForChannel(channel)
    }
}
