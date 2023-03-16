package com.kt.apps.media.xemtv.presenter

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.*
import androidx.core.view.setPadding
import androidx.leanback.widget.Presenter
import com.kt.apps.core.utils.changeWithAlpha
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.core.utils.getMainColor
import com.kt.apps.core.utils.loadImageBitmap
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.xemtv.R
import com.kt.skeleton.setCornerRadius

class FootballPresenter(private val showLeagueTitle: Boolean = true) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val view = LayoutInflater.from(parent!!.context)
            .inflate(R.layout.item_football_presenter, parent, false)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (item as FootballMatch).apply {
            val home = viewHolder!!.view.findViewById<ImageView>(R.id.home_team)
            val away = viewHolder.view.findViewById<ImageView>(R.id.away_team)
            val matchTitle = viewHolder.view.findViewById<TextView>(R.id.match_name)
            val matchTime = viewHolder.view.findViewById<TextView>(R.id.match_time)
            val league = viewHolder.view.findViewById<TextView>(R.id.league)
            val cardView = viewHolder.view.findViewById<FrameLayout>(R.id.card_view)
            cardView.clipToOutline = true
            matchTitle.isSelected = true
            matchTitle.text = this.getMatchName()
            if (this.isLiveMatch) {
                matchTime.setText(com.kt.apps.core.R.string.football_live_match_title)
                matchTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    com.kt.apps.core.R.drawable.background_live_football_circle,
                    0, 0, 0
                )
            } else {
                matchTime.text = this.kickOffTime
                matchTime.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }
            if (showLeagueTitle) {
                league.text = this.league
            }
            val oldFocusChange = viewHolder.view.onFocusChangeListener
            viewHolder.view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                cardView.isSelected = hasFocus
                oldFocusChange.onFocusChange(v, hasFocus)
            }
            home.loadImageBitmap(
                this.homeTeam.logo,
                filterColor = if (this.homeTeam.name.lowercase().contains("juv")) {
                    Color.WHITE
                } else {
                    0
                }
            ) {
                viewHolder.view.findViewById<View>(R.id.background_home).apply {
                    val mainColor = it.getMainColor().toColor()
                    this.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                            mainColor.changeWithAlpha(0.4f),
                            Color.TRANSPARENT,
                            Color.TRANSPARENT,
                        )
                    ).apply {
                        this.setCornerRadius(
                            topLeft = 9.dpToPx().toFloat(),
                            bottomLeft = 9.dpToPx().toFloat(),
                        )
                    }
                }
            }
            away.loadImageBitmap(
                this.awayTeam.logo,
                filterColor = if (this.awayTeam.name.lowercase().contains("juv")) {
                    Color.WHITE
                } else {
                    0
                }
            ) {
                viewHolder.view.findViewById<View>(R.id.background_away).apply {
                    val mainColor = it.getMainColor().toColor()
                    this.background = GradientDrawable(
                        GradientDrawable.Orientation.RIGHT_LEFT, intArrayOf(
                            mainColor.changeWithAlpha(0.4f),
                            Color.TRANSPARENT,
                            Color.TRANSPARENT,
                        )
                    ).apply {
                        this.setCornerRadius(
                            topRight = 9.dpToPx().toFloat(),
                            bottomRight = 9.dpToPx().toFloat(),
                        )
                    }
                }
            }
        }
    }

    private fun getColorForGradient(it: Bitmap): Long {
        val mainColorIntValue = it.getMainColor()
        val mainColor = mainColorIntValue.toColor()
        return Color.pack(
            mainColor.component1(),
            mainColor.component2(),
            mainColor.component3(),
            0.4f
        )
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
    }
}