package com.kt.apps.media.xemtv.ui.details

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.kt.apps.core.tv.model.TVChannel

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(
            viewHolder: ViewHolder,
            item: Any) {
        val movie = item as TVChannel

        viewHolder.title.text = movie.tvChannelName
        viewHolder.subtitle.text = movie.tvChannelName
        viewHolder.body.text = movie.tvChannelName
    }
}