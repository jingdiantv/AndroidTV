package com.kt.apps.core.utils.leanback

import android.view.View
import com.google.android.material.tabs.TabLayout

fun TabLayout.findCurrentFocusedPosition(): Int {
    var tabFocused = -1
    for (i in 0 until tabCount) {
        if (getTabAt(i)!!.view.isFocused) {
            tabFocused = i
            break
        }
    }
    return tabFocused
}


fun TabLayout.findCurrentFocusedView(): View? {
    val tabFocused = findCurrentFocusedPosition()
    if (tabFocused >= 0) {
        return getTabAt(tabFocused)?.view
    }
    return null
}

fun TabLayout.findCurrentSelectedPosition(): Int {
    var tabSelected = -1
    for (i in 0 until tabCount) {
        if (getTabAt(i)!!.view.isSelected) {
            tabSelected = i
            break
        }
    }
    return tabSelected
}

fun TabLayout.findCurrentSelectedTabView(): View? {
    val tabSelected = findCurrentSelectedPosition()
    if (tabSelected >= 0) {
        return getTabAt(tabSelected)?.view
    }
    return null
}

