package com.kt.apps.core.base.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdsAdapter<T, AD : ViewDataBinding, VB : ViewDataBinding> :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    abstract val adsLayoutRes: Int
    abstract val itemLayoutRes: Int
    abstract val oneBanner: Boolean
    open lateinit var onItemRecyclerViewCLickListener: OnItemRecyclerViewCLickListener<T>
    protected val _listItem by lazy { mutableListOf<T>() }
    val listItem: List<T>
        get() = _listItem

    override fun getItemViewType(position: Int): Int {
        return when {
            isOneBanner() -> {
                if (position == 2 || (_listItem.size < 3 && position == _listItem.size)) {
                    adsLayoutRes
                } else {
                    itemLayoutRes
                }
            }
            else -> {
                if (position % 3 == 0) {
                    adsLayoutRes
                } else {
                    itemLayoutRes
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            adsLayoutRes -> {
                val adsViewBinding = DataBindingUtil.bind<AD>(view)!!
                object : BaseAdsViewHolder<AD>(DataBindingUtil.bind(view)!!) {
                    override fun onBind() {
                        bindAds(adsViewBinding, adapterPosition)
                    }
                }
            }
            itemLayoutRes -> {
                val viewItemBinding = DataBindingUtil.bind<VB>(view)!!
                object : BaseViewHolder<T, VB>(viewItemBinding) {
                    override fun onBind(item: T, position: Int) {
                        bindItem(item, viewItemBinding)
                    }

                }
            }
            else -> throw IllegalStateException("Not support ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BaseAdsViewHolder<*> -> {

                (holder as BaseAdsViewHolder<AD>).onBind()
            }
            is BaseViewHolder<*, *> -> {
                (holder as BaseViewHolder<T, VB>).bindItem(
                    if (isOneBanner()) {
                        getItem(position)
                    } else {
                        _listItem[position - (position / 3) - 1]
                    },
                    if (this::onItemRecyclerViewCLickListener.isInitialized) onItemRecyclerViewCLickListener else null
                )
            }
        }
    }

    open fun getItem(position: Int): T {
        return if (position > 2) {
            _listItem[position - 1]
        } else {
            _listItem[position]
        }
    }

    abstract fun bindItem(item: T, binding: VB)
    abstract fun bindAds(adsBinding: AD, position: Int)

    override fun getItemCount(): Int {
        return if (_listItem.isEmpty()) 0 else if (isOneBanner()) _listItem.size + 1 else _listItem.size + (_listItem.size + _listItem.size / 3 + 1) / 3 + 1
    }

    fun isOneBanner() = if (oneBanner) true else _listItem.size / 3 < 1

    open fun onRefresh(items: List<T>, notifyDataChange: Boolean = true) {
        _listItem.clear()
        _listItem.addAll(items)
        if (notifyDataChange) {
            notifyDataSetChanged()
        }
    }

    open fun onRefresh(items: Array<out T>, notifyDataChange: Boolean = true) {
        onRefresh(items.toList(), notifyDataChange)
    }

    open fun onAdd(item: T) {
        _listItem.add(item)
        notifyItemInserted(_listItem.size)
    }

    open fun onAdd(items: List<T>) {
        _listItem.addAll(items)
        notifyItemRangeInserted(_listItem.size, items.size)
    }

    open fun onDelete(item: T) {
        val index = _listItem.indexOf(item)
        _listItem.removeAt(index)
        notifyItemRemoved(index)
    }

    open fun clearAds() {

    }

    open fun pauseAds() {

    }
}