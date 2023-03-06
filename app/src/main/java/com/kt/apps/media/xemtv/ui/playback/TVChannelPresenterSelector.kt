package com.kt.apps.media.xemtv.ui.playback

import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector

class TVChannelPresenterSelector(activity: FragmentActivity) : PresenterSelector() {
    private var channelPresenter = PlaybackVideoFragment.TVChannelPresenter()
    override fun getPresenter(item: Any?): Presenter {
        return channelPresenter
    }
}