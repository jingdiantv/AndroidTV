package com.kt.apps.core.base.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class PagingAdapter<T,
        AD : ViewDataBinding,
        VB : ViewDataBinding,
        LOADING_DB : ViewDataBinding> : BaseAdsAdapter<T, AD, VB>() {
    var currentState: Int
    abstract val loadingRes: Int

    init {
        currentState = LoadItemState.INIT.value
    }

    val isLoadingState: Boolean
        get() = currentState == LoadItemState.LOADING.value

    val lastPosition: Int
        get() = itemCount - 1

    fun isLastPosition(position: Int): Boolean {
        return position == itemCount - 1
    }

    fun setLoading() {
        currentState = LoadItemState.LOADING.value
        notifyItemRangeInserted(itemCount, 1)
    }

    fun loadMoreItem(items: List<T>) {
        currentState = LoadItemState.LOADED.value
        notifyItemRemoved(itemCount)
        _listItem.addAll(items)
        notifyItemRangeChanged(itemCount, items.size)
    }

    override fun getItemCount(): Int {
        return when (currentState) {
            LoadItemState.LOADING.value -> super.getItemCount() + 1
            LoadItemState.LOADED.value -> super.getItemCount()
            else -> super.getItemCount()
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (currentState == LoadItemState.LOADING.value && position == itemCount - 1) return loadingRes
        return super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        if (viewType == loadingRes) return object : BaseLoadingViewHolder<LOADING_DB>(DataBindingUtil.bind(view)!!) {
            override fun bindLoading() {
                bindLoading(viewBinding)
            }

        }
        return super.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BaseLoadingViewHolder<*>) {
            holder.bindLoading()
        } else {
            super.onBindViewHolder(holder, position)
        }
    }

    abstract fun bindLoading(loadMoreBinding: LOADING_DB)

    enum class LoadItemState(val value: Int) {
        LOADING(1), INIT(2), LOADED(3)
    }

    abstract class BaseLoadingViewHolder<VB : ViewDataBinding>(val viewBinding: VB) :
        RecyclerView.ViewHolder(viewBinding.root) {
        abstract fun bindLoading()
    }
}