package com.kt.apps.media.mobile.ui.main

import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.recyclerview.widget.GridLayoutManager
import com.kt.apps.core.Constants
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.core.base.adapter.OnItemRecyclerViewCLickListener
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.core.utils.loadImgByUrl
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemChannelBinding
import com.kt.apps.media.mobile.databinding.ItemRowChannelBinding

class TVDashboardAdapter : BaseAdapter<Pair<String, List<TVChannel>>, ItemRowChannelBinding>() {
    var spanCount = 3
    var onChildItemClickListener: OnItemRecyclerViewCLickListener<TVChannel>? = null
    override val itemLayoutRes: Int
        get() = R.layout.item_row_channel

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<Pair<String, List<TVChannel>>, ItemRowChannelBinding> {
        return super.onCreateViewHolder(parent, viewType)
    }

    override fun bindItem(
        item: Pair<String, List<TVChannel>>,
        binding: ItemRowChannelBinding,
        position: Int,
        holder: BaseViewHolder<Pair<String, List<TVChannel>>, ItemRowChannelBinding>
    ) {
        Logger.d(this, message = "${binding.tvChannelChildRecyclerView.width}")
        binding.title.text = item.first
        val layoutManager = object : GridLayoutManager(
            binding.root.context,
            spanCount,
            VERTICAL,
            false
        ) {
            override fun canScrollVertically(): Boolean {
                return false
            }

            override fun canScrollHorizontally(): Boolean {
                return false
            }
        }
        layoutManager.initialPrefetchItemCount = item.second.size
        layoutManager.isItemPrefetchEnabled = true
        binding.tvChannelChildRecyclerView.layoutManager = layoutManager
        binding.tvChannelChildRecyclerView.setHasFixedSize(true)
        binding.tvChannelChildRecyclerView.clearOnScrollListeners()
        binding.tvChannelChildRecyclerView.adapter = ChildChannelAdapter().apply {
            onRefresh(item.second)
            this.onItemRecyclerViewCLickListener = { item, childPosition ->
                onChildItemClickListener?.invoke(item, position + childPosition)
            }
        }
    }


    override fun onViewRecycled(holder: BaseViewHolder<Pair<String, List<TVChannel>>, ItemRowChannelBinding>) {
        super.onViewRecycled(holder)
        holder.cacheItem = holder.viewBinding.tvChannelChildRecyclerView.adapter
        Logger.d(this, message = "OnView recycler")
    }


    class ChildChannelAdapter : BaseAdapter<TVChannel, ItemChannelBinding>() {
        override val itemLayoutRes: Int
            get() = R.layout.item_channel

        override fun bindItem(
            item: TVChannel,
            binding: ItemChannelBinding,
            position: Int,
            holder: BaseViewHolder<TVChannel, ItemChannelBinding>
        ) {
            binding.item = item
            binding.title.isSelected = true
            Constants.mapChannel[item.tvChannelName]?.let {
                binding.logo.loadImgByDrawableIdResName(it, item.logoChannel)
            } ?: binding.logo.loadImgByUrl(item.logoChannel)
        }

        override fun onViewRecycled(holder: BaseViewHolder<TVChannel, ItemChannelBinding>) {
            super.onViewRecycled(holder)
            holder.viewBinding.logo.setImageBitmap(null)
        }
    }
}