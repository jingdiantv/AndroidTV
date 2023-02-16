package com.kt.skeleton

import android.os.Handler
import android.os.Looper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout

class ViewPager2Skeleton(
    val viewpager: ViewPager2,
    val tabLayout: TabLayout,
    val actualAdapter: FragmentStateAdapter?,
    val fragment: Fragment,
    val tabLayoutRes: Int,
    val itemLayoutRes: Int
) : KunSkeleton.SkeletonScreen {
    private var startTime: Long = 0

    private var defItemCount: Int = 4
    private val skeletonAdapter by lazy {
        object : FragmentStateAdapter(fragment) {
            override fun getItemCount(): Int = defItemCount
            override fun createFragment(position: Int): Fragment =
                ViewPagerSkeleton.DefFragment(itemLayoutRes)
        }
    }
    private val skeletonTabLayout by lazy {
        KunSkeleton.bind(tabLayout)
            .layout(tabLayoutRes)
            .build()
    }

    class Builder(val viewpager: ViewPager2, val tabLayout: TabLayout, val fragment: Fragment) {
        private var adapter: FragmentStateAdapter? = null
        private var tabBarLayoutRes: Int = R.layout.skeleton_tab_layout
        private var viewPagerItemRes: Int = R.layout.skeleton_image_item_two_lines
        fun adapter(adapter: FragmentStateAdapter?): Builder {
            this.adapter = adapter
            return this
        }

        fun tabLayout(@LayoutRes layout: Int?): Builder {
            layout?.let {
                this.tabBarLayoutRes = it
            }
            return this
        }

        fun fragmentItemLayout(@LayoutRes layout: Int?): Builder {
            layout?.let {
                this.viewPagerItemRes = it
            }
            return this
        }

        fun build(): ViewPager2Skeleton = ViewPager2Skeleton(
            viewpager,
            tabLayout,
            adapter,
            fragment,
            tabBarLayoutRes,
            viewPagerItemRes
        )

        fun run() = build().run()
        fun run(onRun: OnRunListener?) = build().run(onRun)
    }

    override fun run(onRun: OnRunListener?) {
        startTime = System.currentTimeMillis()
        viewpager.adapter = skeletonAdapter
        skeletonTabLayout.run(onRun)
        onRun?.invoke()
    }

    override fun hide(onHide: (() -> Unit)?) {
        val duration = System.currentTimeMillis() - startTime
        Handler(Looper.getMainLooper()).postDelayed({
            skeletonTabLayout.hide()
            actualAdapter?.let {
                viewpager.adapter = it
            }
            onHide?.let { it.invoke() }
        }, if (duration > 1000) 0 else 1000 - duration)
        onHide?.invoke()
    }
}