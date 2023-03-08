package com.kt.apps.media.xemtv.ui.main

import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.Row
import com.kt.apps.media.xemtv.ui.football.FootballFragment
import com.kt.apps.media.xemtv.ui.tv.FragmentTVDashboard

class DashboardPageRowFactory(
    private val backgroundManager: BackgroundManager
) : BrowseSupportFragment.FragmentFactory<Fragment>() {
    override fun createFragment(row: Any?): Fragment {
        val rowId = (row as? Row)?.id ?: throw IllegalStateException("Null row id")
        backgroundManager.drawable = null
        return when (rowId) {
            ROW_TV -> FragmentTVDashboard()
            ROW_FOOTBALL -> FootballFragment()
            ROW_SETTING -> FragmentTVDashboard()
            else -> throw IllegalStateException("Not support row")
        }
    }

    companion object {
        const val ROW_TV = 1L
        const val ROW_FOOTBALL = 2L
        const val ROW_SETTING = 3L
    }
}