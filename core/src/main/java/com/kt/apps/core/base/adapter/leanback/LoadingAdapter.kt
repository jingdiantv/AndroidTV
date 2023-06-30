package com.kt.apps.core.base.adapter.leanback

import com.kt.apps.core.base.leanback.ArrayObjectAdapter
import com.kt.apps.core.base.leanback.ListRow
import com.kt.apps.core.base.leanback.Presenter
import com.kt.apps.core.base.presenter.DefaultLoadingPresenter
import com.kt.apps.core.base.presenter.LoadingPresenter

class LoadingAdapter(val presenter: Presenter = DefaultLoadingPresenter()) : ArrayObjectAdapter(presenter) {

    companion object {
        val defaultListRowItems by lazy {
            listOf(5, 3, 4, 1)
        }
    }
}

fun ArrayObjectAdapter.applyLoading(resLayout: Int): ArrayObjectAdapter {
    this.clear()
    LoadingAdapter.defaultListRowItems.forEach {
        val loadingAdapter = LoadingAdapter(object : LoadingPresenter() {
            override val layoutResId: Int
                get() = resLayout

        })
        for (i in 0 until it) {
            loadingAdapter.add(i)
        }
        this.add(ListRow(loadingAdapter))
    }
    return this
}

fun ArrayObjectAdapter.applyLoading(): ArrayObjectAdapter {
    this.clear()
    LoadingAdapter.defaultListRowItems.forEach {
        val loadingAdapter = LoadingAdapter()
        for (i in 0 until it) {
            loadingAdapter.add(i)
        }
        this.add(ListRow(loadingAdapter))
    }
    return this
}