package com.kt.apps.core.base.presenter

import com.kt.apps.core.R

class DefaultLoadingPresenter : LoadingPresenter() {
    override val layoutResId: Int
        get() = R.layout.item_loading_presenter

    companion object {
    }
}