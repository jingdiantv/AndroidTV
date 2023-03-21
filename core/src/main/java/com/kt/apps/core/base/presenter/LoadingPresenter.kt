package com.kt.apps.core.base.presenter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.leanback.widget.Presenter
import com.kt.skeleton.runAnimationChangeBackground

abstract class LoadingPresenter : Presenter() {

    abstract val layoutResId: Int

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val loadingView = LayoutInflater.from(parent?.context)
            .inflate(layoutResId, parent, false)

        return LoadingViewHolder(loadingView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (viewHolder?.view as? ViewGroup)?.forEach {
            it.runAnimationChangeBackground()
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
    }

    class LoadingViewHolder(view: View) : ViewHolder(view) {
    }
}