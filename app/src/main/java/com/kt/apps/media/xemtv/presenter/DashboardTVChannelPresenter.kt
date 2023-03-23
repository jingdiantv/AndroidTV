package com.kt.apps.media.xemtv.presenter

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper

import com.kt.apps.core.Constants
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.core.utils.loadImgByUrl
import com.kt.apps.media.xemtv.R
import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
class DashboardTVChannelPresenter : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        Log.d(TAG, "onCreateViewHolder")

        sDefaultBackgroundColor = Color.TRANSPARENT
        sSelectedBackgroundColor = Color.TRANSPARENT
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, com.kt.apps.core.R.drawable.app_icon)

        val wrapper = ContextThemeWrapper(parent.context, R.style.ImageCardViewStyleTitle)
        val cardView = object : ImageCardView(wrapper) {
            override fun setSelected(selected: Boolean) {
                updateCardBackgroundColor(this, selected)
                super.setSelected(selected)
            }

            override fun invalidate() {
                super.invalidate()
            }
        }


        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val movie = item as TVChannel
        val cardView = viewHolder.view as ImageCardView

        Log.d(TAG, "onBindViewHolder")
        cardView.titleText = movie.tvChannelName
        cardView.contentText = null
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        cardView.let { imgView ->
            val channelName = item.tvChannelName
            val name = Constants.mapChannel[channelName]

            name?.let {
                imgView.mainImageView
                    .loadImgByDrawableIdResName(it, item.logoChannel)
            } ?: imgView.mainImageView.loadImgByUrl(item.logoChannel)
        }
        updateCardBackgroundColor(cardView, false)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        Log.d(TAG, "onUnbindViewHolder")
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        view.findViewById<TextView>(androidx.leanback.R.id.title_text)
            .background = null
        view.background = null
        view.infoAreaBackground = null
    }

    companion object {
        private const val TAG = "CardPresenter"

        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }
}