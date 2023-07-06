package com.kt.apps.media.xemtv.presenter

import androidx.fragment.app.FragmentActivity
import com.kt.apps.core.base.leanback.Presenter
import com.kt.apps.core.base.leanback.PresenterSelector
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.football.model.FootballMatch

class TVChannelPresenterSelector(
    activity: FragmentActivity,
    private val playback: Boolean = false
) : PresenterSelector() {
    private val presenterMap by lazy {
        mutableMapOf<String, Presenter>()
    }

    var defaultImageWidthDimensions: Float? = null
    override fun getPresenter(item: Any?): Presenter {
        item ?: throw IllegalStateException("Null item")
        val presenter: Presenter? = presenterMap[item::class.java.name]
        return presenter ?: when (item) {
            is TVChannel, is ExtensionsChannel -> if (playback) {
                PlaybackTVChannelPresenter()
            } else {
                DashboardTVChannelPresenter().apply {
                    this.defaultImageWidthDimensions = this@TVChannelPresenterSelector.defaultImageWidthDimensions
                }
            }
            is FootballMatch -> FootballPresenter()
            else -> throw IllegalStateException("Not support presenter for: ${item::class.java.name}")
        }.also {
            presenterMap[item::class.java.name] = it
        }
    }

}