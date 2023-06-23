package com.kt.apps.media.xemtv.presenter

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.xemtv.R
import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
class PlaybackTVChannelPresenter : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        sDefaultBackgroundColor = Color.TRANSPARENT
        sSelectedBackgroundColor = Color.TRANSPARENT
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, com.kt.apps.core.R.drawable.app_icon)

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_playback, null, false)
        return PlaybackViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {

        when (item) {
            is TVChannel -> {
                (viewHolder as PlaybackViewHolder).onBind(item)
            }

        }

    }

    class PlaybackViewHolder(val view: View) : ViewHolder(view) {
        private val titleView: TextView = view.findViewById(R.id.title)

        fun onBind(item: TVChannel) {
            titleView.text = item.tvChannelName
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }


    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }
}