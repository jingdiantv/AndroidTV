package com.kt.apps.media.xemtv.presenter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.request.RequestOptions
import com.kt.apps.core.Constants
import com.kt.apps.core.GlideApp
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.core.utils.loadImgByUrl
import com.kt.apps.media.xemtv.App
import com.kt.apps.media.xemtv.R

class TVChannelPresenter() : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val view = LayoutInflater.from(parent!!.context)
            .inflate(R.layout.item_channel_overlay, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        viewHolder?.view
            ?.findViewById<ImageCardView>(R.id.logoChannel)
            ?.mainImageView?.let { imgView ->
                val channelName = (item as TVChannel).tvChannelName
                val name = Constants.mapChannel[channelName]
                name?.let {
                    imgView.loadImgByDrawableIdResName(it, item.logoChannel)
                } ?: imgView.loadImgByUrl(item.logoChannel)
            }
        viewHolder?.view
            ?.findViewById<TextView>(R.id.channel_name)
            ?.text = (item as TVChannel).tvChannelName
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
    }

}