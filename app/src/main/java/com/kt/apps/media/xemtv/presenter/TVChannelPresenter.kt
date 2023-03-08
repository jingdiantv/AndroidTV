package com.kt.apps.media.xemtv.presenter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.kt.apps.core.GlideApp
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.xemtv.R

class TVChannelPresenter() : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val view = LayoutInflater.from(parent!!.context)
            .inflate(R.layout.item_channel_overlay, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        viewHolder?.view?.findViewById<ImageCardView>(R.id.logoChannel)
            ?.let {
                GlideApp.with(it)
                    .load((item as TVChannel).logoChannel)
                    .optionalCenterInside()
                    .into(it.mainImageView)
            }
        viewHolder?.view?.findViewById<TextView>(R.id.channel_name)
            ?.text = (item as TVChannel).tvChannelName
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
    }

}