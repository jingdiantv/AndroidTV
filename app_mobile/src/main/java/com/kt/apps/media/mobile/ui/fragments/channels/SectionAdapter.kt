package com.kt.apps.media.mobile.ui.fragments.channels

import android.graphics.drawable.Drawable
import com.kt.apps.core.base.adapter.BaseAdapter
import com.kt.apps.core.base.adapter.BaseViewHolder
import com.kt.apps.media.mobile.R
import com.kt.apps.media.mobile.databinding.ItemSectionBinding

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

class SectionAdapter: BaseAdapter<SectionItem, ItemSectionBinding>() {
    override val itemLayoutRes: Int
        get() = R.layout.item_section
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
    }

    fun selectForId(id: Int) {
        listItem.firstOrNull {
            it.id == id
        }?.run {
            currentSelectedItem = this
        }
    }
}