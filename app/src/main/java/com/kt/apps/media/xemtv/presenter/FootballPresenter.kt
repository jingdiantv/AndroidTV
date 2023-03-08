package com.kt.apps.media.xemtv.presenter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.kt.apps.core.GlideApp
import com.kt.apps.core.utils.formatDateTime
import com.kt.apps.core.utils.toDate
import com.kt.apps.football.model.FootballMatch
import com.kt.apps.media.xemtv.R
import java.util.Calendar

class FootballPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val view = LayoutInflater.from(parent!!.context)
            .inflate(R.layout.item_football_presenter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (item as FootballMatch).apply {
            val home = viewHolder!!.view.findViewById<ImageView>(R.id.home_team)
            val away = viewHolder.view.findViewById<ImageView>(R.id.away_team)
            val matchTitle = viewHolder.view.findViewById<TextView>(R.id.match_name)
            val matchTime = viewHolder.view.findViewById<TextView>(R.id.match_time)
            matchTitle.text = this.getMatchName()
            matchTime.text = this.kickOffTime
            GlideApp.with(home)
                .load(this.homeTeam.logo)
                .centerInside()
                .into(home)

            GlideApp.with(away)
                .load(this.awayTeam.logo)
                .centerInside()
                .into(away)

        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
    }
}