package com.kt.apps.media.mobile.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
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
import com.kt.apps.core.utils.fadeIn
import com.kt.apps.core.utils.fadeOut
import com.kt.apps.core.utils.gone
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ActivityMainBinding
import com.kt.apps.media.mobile.ui.playback.PlaybackActivity
import com.kt.skeleton.KunSkeleton
import javax.inject.Inject

class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val adapter by lazy {
        TVDashboardAdapter().apply {
            this.onChildItemClickListener = { item, position ->
                tvChannelViewModel.getLinkStreamForChannel(item)
            }
        }
    }

    private val onItemSelected by lazy {
        NavigationBarView.OnItemSelectedListener {
            when (it.itemId) {
                R.id.radio -> {
                    if (binding.navigationRailView.selectedItemId != R.id.radio) {
                        Logger.d(this@MainActivity, "OnSelectedChange", "Radio")
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
                    if (binding.navigationRailView.selectedItemId != R.id.tv) {
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
                    if (binding.navigationRailView.selectedItemId != R.id.radio && System.currentTimeMillis() - lastDetectedTime > 300) {
                        Logger.d(this@MainActivity, message = "Change item Radio")
                        binding.navigationRailView.setOnItemSelectedListener(null)
                        lastDetectedTime = System.currentTimeMillis()
                        binding.navigationRailView.findViewById<View>(R.id.radio)
                            .performClick()
                        binding.navigationRailView.setOnItemSelectedListener(onItemSelected)
                    }
                } else {
                    if (binding.navigationRailView.selectedItemId != R.id.tv && System.currentTimeMillis() - lastDetectedTV > 300) {
                        lastDetectedTV = System.currentTimeMillis()
                        Logger.d(this@MainActivity, message = "Change item TV")
                        binding.navigationRailView.setOnItemSelectedListener(null)
                        binding.navigationRailView.findViewById<View>(R.id.tv)
                            .performClick()
                        binding.navigationRailView.setOnItemSelectedListener(onItemSelected)
                    }
                }
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
            tvChannelViewModel.getListTVChannel(true)
        }
        binding.mainChannelRecyclerView.addOnScrollListener(onScrollListener!!)
        binding.mainChannelRecyclerView.setHasFixedSize(true)
        binding.navigationRailView.setOnItemSelectedListener(onItemSelected)
        tvChannelViewModel.tvChannelLiveData.observe(this) {
            when (it) {
                is DataState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    skeletonScreen.hide {
                        adapter.onRefresh(
                            it.data.groupBy {
                                it.tvGroupLocalName
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

    override fun onStop() {
        binding.progressDialog.fadeOut {
            binding.progressDialog.gone()
            progressHelper.stopSpinning()
        }
        super.onStop()
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