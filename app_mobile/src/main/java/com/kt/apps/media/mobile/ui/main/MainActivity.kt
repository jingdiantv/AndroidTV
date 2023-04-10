package com.kt.apps.media.mobile.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import cn.pedant.SweetAlert.ProgressHelper
import com.google.android.material.navigation.NavigationBarView
import com.kt.apps.core.Constants
import com.kt.apps.core.base.BaseActivity
import com.kt.apps.core.base.DataState
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.core.utils.gone
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityMainBinding
import com.kt.apps.media.mobile.services.TVService
import com.kt.apps.media.mobile.ui.playback.ITVServiceAidlInterface
import com.kt.apps.media.mobile.ui.playback.PlaybackActivity
import com.kt.skeleton.KunSkeleton
import javax.inject.Inject

class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private var service: ITVServiceAidlInterface? = null
    private var iServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.e("TAG", "onServiceConnected")
            this@MainActivity.service = ITVServiceAidlInterface.Stub.asInterface(service!!)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.e("TAG", "onServiceDisconnected")

        }

    }

    private val adapter by lazy {
        TVDashboardAdapter().apply {
            this.onChildItemClickListener = { item, position ->
                tvChannelViewModel.getLinkStreamForChannel(item)
            }
        }
    }

    private var navigationRailView: NavigationBarView? = null

    private val onItemSelected by lazy {
        NavigationBarView.OnItemSelectedListener {
            when (it.itemId) {
                R.id.radio -> {
                    Logger.d(this@MainActivity, "OnSelectedChange", "Radio")
                    service?.sendData("Radio")
                    if (navigationRailView?.selectedItemId != R.id.radio) {
                        onScrollListener?.let { it1 -> binding.mainChannelRecyclerView.removeOnScrollListener(it1) }
                        binding.mainChannelRecyclerView.smoothScrollToPosition(8)
                        binding.mainChannelRecyclerView.addOnScrollListener(object : OnScrollListener() {
                            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                                super.onScrollStateChanged(recyclerView, newState)
                                if (newState == SCROLL_STATE_IDLE) {
                                    onScrollListener?.let { it1 ->
                                        binding.mainChannelRecyclerView.addOnScrollListener(
                                            it1
                                        )
                                    }
                                    binding.mainChannelRecyclerView.removeOnScrollListener(this)
                                }
                            }
                        })
                    }

                }

                R.id.tv -> {
                    if (navigationRailView?.selectedItemId != R.id.tv) {
                        Logger.d(this@MainActivity, "OnSelectedChange", "TV")
                        onScrollListener?.let { it1 -> binding.mainChannelRecyclerView.removeOnScrollListener(it1) }
                        binding.mainChannelRecyclerView.smoothScrollToPosition(0)
                        binding.mainChannelRecyclerView.addOnScrollListener(object : OnScrollListener() {
                            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                                super.onScrollStateChanged(recyclerView, newState)
                                if (newState == SCROLL_STATE_IDLE) {
                                    onScrollListener?.let { it1 ->
                                        binding.mainChannelRecyclerView.addOnScrollListener(
                                            it1
                                        )
                                    }
                                    binding.mainChannelRecyclerView.removeOnScrollListener(this)
                                }
                            }
                        })
                    }
                }
            }
            return@OnItemSelectedListener true
        }
    }
    private var onScrollListener: OnScrollListener? = null

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
                    if (navigationRailView?.selectedItemId != R.id.radio && System.currentTimeMillis() - lastDetectedTime > 300) {
                        Logger.d(this@MainActivity, message = "Change item Radio")
                        navigationRailView?.setOnItemSelectedListener(null)
                        lastDetectedTime = System.currentTimeMillis()
                        navigationRailView?.selectedItemId = R.id.radio
                        navigationRailView?.setOnItemSelectedListener(onItemSelected)
                    }
                } else {
                    if (navigationRailView?.selectedItemId != R.id.tv && System.currentTimeMillis() - lastDetectedTV > 300) {
                        lastDetectedTV = System.currentTimeMillis()
                        Logger.d(this@MainActivity, message = "Change item TV")
                        navigationRailView?.setOnItemSelectedListener(null)
                        navigationRailView?.selectedItemId = R.id.tv
                        navigationRailView?.setOnItemSelectedListener(onItemSelected)
                    }
                }
//                if (itemTitle == TVChannelGroup.VOV.value || itemTitle == TVChannelGroup.VOH.value) {
//                    if (navigationRailView?.selectedItemId != R.id.radio && System.currentTimeMillis() - lastDetectedTime > 300) {
//                        Logger.d(this@MainActivity, message = "Change item Radio")
//                        navigationRailView?.setOnItemSelectedListener(null)
//                        lastDetectedTime = System.currentTimeMillis()
//                        navigationRailView?.selectedItemId = R.id.radio
//                        navigationRailView?.setOnItemSelectedListener(onItemSelected)
//                    }
//                } else {
//                    if (navigationRailView?.selectedItemId != R.id.tv && System.currentTimeMillis() - lastDetectedTV > 300) {
//                        lastDetectedTV = System.currentTimeMillis()
//                        Logger.d(this@MainActivity, message = "Change item TV")
//                        navigationRailView?.setOnItemSelectedListener(null)
//                        navigationRailView?.selectedItemId = R.id.tv
//                        navigationRailView?.setOnItemSelectedListener(onItemSelected)
//                    }
//                }
            }
        }
    }

    private val tvChannelViewModel by lazy {
        ViewModelProvider(this, factory)[TVChannelViewModel::class.java]
    }

    private val skeletonScreen by lazy {
        KunSkeleton.bind(binding.mainChannelRecyclerView)
            .adapter(adapter)
            .recyclerViewLayoutItem(R.layout.item_row_channel_skeleton, R.layout.item_channel_skeleton)
            .build()
    }

    private val progressHelper by lazy {
        ProgressHelper(this)
    }

    override val layoutRes: Int
        get() = R.layout.activity_main

    override fun initView(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            tvChannelViewModel.getListTVChannel(true)
        }
        navigationRailView = binding.navigationRailView as NavigationBarView
        binding.mainChannelRecyclerView.isNestedScrollingEnabled = false
        binding.mainChannelRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            this.isItemPrefetchEnabled = true
            this.initialPrefetchItemCount = 9
        }
        binding.mainChannelRecyclerView.setHasFixedSize(true)
        binding.mainChannelRecyclerView.setItemViewCacheSize(9)
        binding.mainChannelRecyclerView.viewTreeObserver
            .addOnGlobalLayoutListener {
                adapter.spanCount =
                    3.coerceAtLeast((binding.mainChannelRecyclerView.width / 170 / resources.displayMetrics.scaledDensity).toInt())
            }
        binding.mainChannelRecyclerView.adapter = adapter
        progressHelper.progressWheel = binding.progressWheel
    }

    override fun initAction(savedInstanceState: Bundle?) {
        onScrollListener = _onScrollListener
        binding.swipeRefreshLayout.setOnRefreshListener {
            tvChannelViewModel.getListTVChannel(true, TVDataSourceFrom.MAIN_SOURCE)
        }
        binding.mainChannelRecyclerView.addOnScrollListener(onScrollListener!!)
        binding.mainChannelRecyclerView.setHasFixedSize(true)
        navigationRailView?.setOnItemSelectedListener(onItemSelected)
        tvChannelViewModel.tvChannelLiveData.observe(this) {
            when (it) {
                is DataState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    skeletonScreen.hide {
                        adapter.onRefresh(
                            it.data.groupBy {
                                it.tvGroup
                            }.toList()
                                .sortedWith(Comparator { o1, o2 ->
                                    if (o2.first == TVChannelGroup.VOV.value || o2.first == TVChannelGroup.VOH.value) {
                                        if (o1.first == TVChannelGroup.VOH.value) {
                                            return@Comparator 0
                                        }
                                        return@Comparator -1
                                    }
                                    return@Comparator 1
                                })
                        )
                    }
                }

                is DataState.Loading -> {
                    skeletonScreen.run()
                }

                else -> {

                }
            }
        }

        tvChannelViewModel.tvWithLinkStreamLiveData.observe(this) {
            when (it) {
                is DataState.Success -> {
                    binding.progressDialog.fadeOut {
                        progressHelper.stopSpinning()
                    }
                    val intent = Intent(this, PlaybackActivity::class.java)
                    intent.putExtra(Constants.EXTRA_TV_CHANNEL, it.data)
                    intent.putExtra(Constants.EXTRA_PLAYBACK_TYPE, PlaybackActivity.Type.TV as Parcelable)
                    startActivity(intent)
                }

                is DataState.Loading -> {
                    binding.progressDialog.fadeIn {
                        progressHelper.spin()
                    }

                }

                else -> {
                    binding.progressDialog.fadeOut {
                        progressHelper.stopSpinning()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent()
        intent.component = ComponentName("com.kt.apps.media.mobile.xemtv", "com.kt.apps.media.mobile.services.TVService")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
        bindService(intent, iServiceConnection, Context.BIND_IMPORTANT)


    }

    override fun onResume() {
        super.onResume()
        navigationRailView = binding.navigationRailView as NavigationBarView
    }


    override fun onStop() {
        binding.progressDialog.fadeOut {
            binding.progressDialog.gone()
            progressHelper.stopSpinning()
        }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        navigationRailView = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onBackPressed() {
        if (binding.progressDialog.isVisible) {
            binding.progressWheel.fadeOut {
                progressHelper.stopSpinning()
            }
        } else {
            super.onBackPressed()
        }
    }

}