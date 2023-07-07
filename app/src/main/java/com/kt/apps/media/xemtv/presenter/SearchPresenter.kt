package com.kt.apps.media.xemtv.presenter

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kt.apps.core.Constants
import com.kt.apps.core.base.leanback.ImageCardView
import com.kt.apps.core.base.leanback.Presenter
import com.kt.apps.core.usecase.search.SearchForText
import com.kt.apps.core.utils.getKeyForLocalLogo
import com.kt.apps.core.utils.loadImgByDrawableIdResName
import com.kt.apps.core.utils.loadImgByUrl
import com.kt.apps.core.utils.replaceVNCharsToLatinChars
import kotlin.properties.Delegates

class SearchPresenter : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()
    private var _filterHighlight: List<String>? = null

    var filterString: String?
        set(value) {
            _filterHighlight = value?.lowercase()
                ?.replaceVNCharsToLatinChars()
                ?.split(" ")
                ?.filter {
                    it.isNotBlank()
                }
        }
        get() = _filterHighlight?.reduce { acc, s ->
            "$acc $s"
        }

    val filterKeyWords: List<String>?
        get() = _filterHighlight

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        sDefaultBackgroundColor = Color.TRANSPARENT
        sSelectedBackgroundColor = Color.TRANSPARENT
        mDefaultCardImage = ContextCompat.getDrawable(
            parent.context,
            com.kt.apps.resources.R.drawable.app_icon
        )
        val cardView: ImageCardView = DashboardTVChannelPresenter.TVImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView

        when (item) {
            is SearchForText.SearchResult.ExtensionsChannelWithCategory -> {
                cardView.titleText = item.highlightTitle
                cardView.contentText = ""
                cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
                updateCardBackgroundColor(cardView, false)
                cardView.let { imgView ->
                    val name = Constants.mapChannel[
                            item.data
                                .tvChannelName
                                .getKeyForLocalLogo()
                    ]
                    imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    name?.let {
                        imgView.mainImageView
                            .loadImgByDrawableIdResName(it, item.data.logoChannel.trim())
                    } ?: imgView.mainImageView.loadImgByUrl(item.data.logoChannel.trim())
                }
            }

            is SearchForText.SearchResult.TV -> {
                cardView.titleText = item.highlightTitle
                cardView.contentText = null
                cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

                cardView.let { imgView ->
                    imgView.mainImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    imgView.mainImageView
                        .loadImgByDrawableIdResName(item.data.logoChannel, item.data.logoChannel)
                }
                updateCardBackgroundColor(cardView, false)
            }
        }

    }

    private fun getHighlightTitle(realTitle: String): SpannableString {
        return getHighlightTitle(realTitle, _filterHighlight)
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
        private val HIGH_LIGHT_COLOR by lazy {
            Color.parseColor("#fb8500")
        }
        private val FOREGROUND_HIGH_LIGHT_COLOR by lazy {
            ForegroundColorSpan(HIGH_LIGHT_COLOR)
        }

        fun getHighlightTitle(realTitle: String, _filterHighlight: List<String>?): SpannableString {
            val spannableString = SpannableString(realTitle)
            val lowerRealTitle = realTitle.trim()
                .lowercase()
                .replaceVNCharsToLatinChars()

            val titleLength = lowerRealTitle.length
            _filterHighlight?.forEach { searchKey ->
                var index = lowerRealTitle.indexOf(searchKey)
                while (index > -1 && index + searchKey.length <= titleLength) {
                    spannableString.setSpan(
                        ForegroundColorSpan(HIGH_LIGHT_COLOR),
                        index,
                        index + searchKey.length,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                    )
                    val startIndex = index + searchKey.length
                    if (startIndex >= titleLength) {
                        break
                    }
                    index = lowerRealTitle.indexOf(searchKey, startIndex)
                }
            }
            return spannableString
        }
    }
}