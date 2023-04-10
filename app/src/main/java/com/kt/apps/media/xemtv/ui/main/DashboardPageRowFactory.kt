package com.kt.apps.media.xemtv.ui.main

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.Row
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.R
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.media.xemtv.ui.extensions.FragmentAddExtensions
import com.kt.apps.media.xemtv.ui.extensions.FragmentExtensions
import com.kt.apps.media.xemtv.ui.football.FootballFragment
import com.kt.apps.media.xemtv.ui.radio.RadioFragment
import com.kt.apps.media.xemtv.ui.tv.FragmentTVDashboard

typealias OnFragmentChange = (pageID: Long) -> Unit
class DashboardPageRowFactory(
    private val backgroundManager: BackgroundManager,
    var listExtensions: List<ExtensionsConfig>
) : BrowseSupportFragment.FragmentFactory<Fragment>() {
    var onFragmentChangeListener: OnFragmentChange? = null
    override fun createFragment(row: Any?): Fragment {
        val rowId = (row as? Row)?.id ?: throw IllegalStateException("Null row id")
        Logger.d(this, tag = "createFragment", message = "$rowId")
        backgroundManager.drawable = null
        onFragmentChangeListener?.invoke(rowId)
        return when (rowId) {
            ROW_TV -> {
                backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.tv_bg)
                FragmentTVDashboard()
            }
            ROW_FOOTBALL -> {
                backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.bg_football)
                FootballFragment()
            }
            ROW_RADIO -> {
                backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.tv_bg)
                RadioFragment()
            }
            ROW_ADD_EXTENSION -> {
                return FragmentAddExtensions()
            }
            else -> {
                try {
                    backgroundManager.drawable = ContextCompat.getDrawable(CoreApp.getInstance(), R.drawable.tv_bg)
                    FragmentExtensions.newInstance(listExtensions[rowId.toInt()].sourceUrl)
                } catch (e: Exception) {
                    throw IllegalStateException("Not support row")
                }
            }
        }
    }

    companion object {
        const val ROW_TV = 10999L
        const val ROW_FOOTBALL = 10998L
        const val ROW_RADIO = 10997L
        const val ROW_ADD_EXTENSION = 10996L
    }
}