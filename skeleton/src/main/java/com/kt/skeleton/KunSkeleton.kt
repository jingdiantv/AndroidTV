package com.kt.skeleton

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout

typealias OnRunListener = () -> Unit

@FunctionalInterface
interface OnSkeletonRunListener {
    fun onRun()
}

object KunSkeleton {
    const val TAG = "Skeleton screen"

    @JvmStatic
    fun bind(recyclerView: RecyclerView): RecyclerViewSkeletonScreen.Builder {
        return RecyclerViewSkeletonScreen.Builder(recyclerView)
    }

    @JvmStatic
    fun bind(view: View): ViewSkeletonScreen.Builder {
        return ViewSkeletonScreen.Builder(view)
    }

    @JvmStatic
    fun bind(
        view: ViewPager,
        tab: TabLayout,
        fragmentManager: FragmentManager
    ): ViewPagerSkeleton.Builder {
        return ViewPagerSkeleton.Builder(view, tab, fragmentManager)
    }

    @JvmStatic
    fun bind(view: ViewPager2, tab: TabLayout, fragment: Fragment): ViewPager2Skeleton.Builder {
        return ViewPager2Skeleton.Builder(view, tab, fragment)
    }


    interface SkeletonScreen {

        fun run(onRun: OnRunListener? = null)

        fun hide(onHide: (() -> Unit)? = null)

    }

}