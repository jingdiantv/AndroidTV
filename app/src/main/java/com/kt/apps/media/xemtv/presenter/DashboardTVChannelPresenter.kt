package com.kt.apps.media.xemtv.presenter

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper

import com.kt.apps.core.Constants
import com.kt.apps.core.extensions.ExtensionsChannel
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
        sDefaultBackgroundColor = Color.TRANSPARENT
        sSelectedBackgroundColor = Color.TRANSPARENT
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, com.kt.apps.core.R.drawable.app_icon)

        val wrapper = ContextThemeWrapper(parent.context, R.style.ImageCardViewStyleTitle)
        val cardView:ImageCardView = object : ImageCardView(wrapper) {
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
        val cardView = viewHolder.view as ImageCardView

        when(item) {
            is TVChannel -> {
                cardView.titleText = item.tvChannelName
                cardView.contentText = null
                cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

                cardView.let { imgView ->
                    imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    imgView.mainImageView
                        .loadImgByDrawableIdResName(item.logoChannel, item.logoChannel)
                }
                updateCardBackgroundColor(cardView, false)
            }

            is ExtensionsChannel -> {
                cardView.titleText = item.tvChannelName
                cardView.contentText = ""
                cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
                updateCardBackgroundColor(cardView, false)
                cardView.mainImageView.setBackgroundResource(com.kt.apps.core.R.drawable.channel_bg)
                cardView.let { imgView ->
                    val channelName = item.tvChannelName
                    val name = Constants.mapChannel[channelName]
                    imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    name?.let {
                        imgView.mainImageView
                            .loadImgByDrawableIdResName(it, item.logoChannel)
                    } ?: imgView.mainImageView.loadImgByUrl(item.logoChannel)
                }
            }
        }

    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        view.findViewById<TextView>(androidx.leanback.R.id.title_text)
            .background = null
        view.background = null
        view.infoAreaBackground = null
    }

    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }
}