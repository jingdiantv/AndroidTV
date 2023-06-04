package com.kt.apps.media.mobile.ui.fragments.channels

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.widget.AdapterView.OnItemLongClickListener
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemSectionBinding
import com.kt.skeleton.convertDpToPixel

interface SectionItem {
    val displayTitle: String
    val id: Int
    val icon: Drawable
}

sealed class SectionItemElement {
    class MenuItem(
        override val displayTitle: String,
        override val id: Int,
        override val icon: Drawable,
    ): SectionItem
}

class SectionAdapter(val context: Context): BaseAdapter<SectionItem, ItemSectionBinding>() {
    override val itemLayoutRes: Int
        get() = R.layout.item_section

    private val maxItemDisplay: Int = 4

    private val isLandscape by lazy { context.resources.getBoolean(R.bool.is_landscape) }

    private var calculatePreferWidth: Int = 0

    var onItemLongCLickListener: (item: SectionItem) -> Boolean = {
        false
    }
    override fun bindItem(
        item: SectionItem,
        binding: ItemSectionBinding,
        position: Int,
        holder: BaseViewHolder<SectionItem, ItemSectionBinding>
    ) {
        binding.title.text = item.displayTitle
        binding.logo.setImageDrawable(item.icon)

        currentSelectedItem?.takeIf {
            it.id == item.id
        }?.run {
            binding.logo.setColorFilter(binding.logo.context.resources.getColor(R.color.white))
            binding.title.setTextColor(binding.title.context.resources.getColor(R.color.white))
        } ?: kotlin.run {
            binding.logo.setColorFilter(binding.logo.context.resources.getColor(R.color.disabled_color))
            binding.title.setTextColor(binding.title.context.resources.getColor(R.color.disabled_color))
        }
        if (!isLandscape) {
            val width = if (calculatePreferWidth > 0)
                calculatePreferWidth
            else
                calculatePreferWidth()
            calculatePreferWidth = width
            binding.itemContainer.layoutParams.width = width
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<SectionItem, ItemSectionBinding>,
        position: Int
    ) {
        super.onBindViewHolder(holder, position)
        holder.itemView.setOnLongClickListener {
            onItemLongCLickListener(listItem[position])
        }
    }

    fun selectForId(id: Int) {
        listItem.firstOrNull {
            it.id == id
        }?.run {
            currentSelectedItem = this
        }
    }

    private fun calculatePreferWidth(): Int {
        val currentItemCount = itemCount
        val currentWidth = context.resources.displayMetrics.widthPixels
        val minItemWidth = convertDpToPixel(96f, context)
        if (minItemWidth * currentItemCount > currentWidth) {
            return minItemWidth.toInt()
        }
        val newItemWidth = currentWidth / currentItemCount
        return newItemWidth
    }

    override fun onRefresh(items: List<SectionItem>, notifyDataSetChange: Boolean) {
        super.onRefresh(items, notifyDataSetChange)
        if (notifyDataSetChange) {
            calculatePreferWidth = 0
        }
    }
}