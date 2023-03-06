package com.kt.apps.media.xemtv.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowHeaderPresenter
import androidx.leanback.widget.RowHeaderView
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.media.xemtv.R

class DashboardIconHeaderPresenterSelector : PresenterSelector() {
    override fun getPresenter(item: Any?): Presenter {
        return HeaderIconPresenter()
    }

    class HeaderIconPresenter() : RowHeaderPresenter() {
        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
            val view = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.header_dashboard, parent, false)
            return HeaderIconViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder?, item: Any?) {
            super.onBindViewHolder(viewHolder, item)
            val headerItem = if (item == null) null else (item as Row).headerItem
            headerItem?.let {
                if (it is HeaderItemWithIcon) {
                    (viewHolder as HeaderIconViewHolder).apply {
                        this.headerView.text = it.name
                    }
                }
            }
            Logger.d(this, message = "Bind header");
        }

        class HeaderIconViewHolder(view: View) : ViewHolder(view) {
            val iconView: ImageView by lazy {
                view.findViewById(R.id.icon)
            }

            val headerView: RowHeaderView by lazy {
                view.findViewById(R.id.row_header)
            }

        }

        class HeaderItemWithIcon(
            private val headerId: Long,
            private val headerName: String,
            val icon: Int
        ) : HeaderItem(headerId, headerName) {}
    }
}