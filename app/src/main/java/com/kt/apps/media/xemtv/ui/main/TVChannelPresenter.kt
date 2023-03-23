package com.kt.apps.media.xemtv.ui.main

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.kt.apps.core.GlideApp
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.xemtv.R
import kotlin.properties.Delegates

class TVChannelPresenter : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, com.kt.apps.core.R.color.default_background)
        sSelectedBackgroundColor = ContextCompat.getColor(parent.context, com.kt.apps.core.R.color.selected_background)
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, com.kt.apps.core.R.drawable.app_icon)

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_overlay, parent, false)
        updateCardBackgroundColor(view.findViewById(R.id.logoChannel), false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        viewHolder?.view?.findViewById<ImageCardView>(R.id.logoChannel)
            ?.let {
                GlideApp.with(it)
                    .load((item as TVChannel).logoChannel)
                    .centerCrop()
                    .error(mDefaultCardImage)
                    .into(it.mainImageView)
            }
        viewHolder?.view?.findViewById<TextView>(R.id.channel_name)
            ?.text = (item as TVChannel).tvChannelName
    }


    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        view.setBackgroundColor(color)
        view.setInfoAreaBackgroundColor(color)
    }


    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
    }
}