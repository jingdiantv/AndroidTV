package com.kt.apps.media.xemtv.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowHeaderView
import com.kt.apps.core.GlideApp
import com.kt.apps.core.base.leanback.RowHeaderPresenter
import com.kt.apps.core.logging.Logger
import com.kt.apps.media.xemtv.R

typealias HeaderItemLongClickListener = ((headerId: Long) -> Unit)

class DashboardIconHeaderPresenterSelector : PresenterSelector() {
    var onHeaderLongClickedListener: HeaderItemLongClickListener? = null
    override fun getPresenter(item: Any?): Presenter {
        return HeaderIconPresenter(onHeaderLongClickedListener)
    }

    class HeaderIconPresenter(
        private val onHeaderLongClickedListener: HeaderItemLongClickListener? = null
    ) : RowHeaderPresenter() {
        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
            val view = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.header_dashboard, parent, false)
            val viewHolder = HeaderIconViewHolder(view)
            setSelectLevel(viewHolder, 0f)
            return viewHolder
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder?, item: Any?) {
            super.onBindViewHolder(viewHolder, item)
            val headerItem = if (item == null) null else (item as Row).headerItem
            headerItem?.let {
                if (it is HeaderItemWithIcon) {
                    (viewHolder as HeaderIconViewHolder).apply {
                        this.view.isLongClickable = true
                        this.view.setOnLongClickListener {
                            Logger.d(this, message = "HeaderID: ${headerItem.id}")
                            onHeaderLongClickedListener?.invoke(headerItem.id)
                            return@setOnLongClickListener true
                        }
                        this.headerView.text = it.name
                        GlideApp.with(this.headerView)
                            .load(it.icon)
                            .into(this.iconView)
                        Logger.d(this, message = "${this.selectLevel}")
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

            init {
                view.isFocusable = true
                view.isFocusableInTouchMode = true
                val oldOnFocus = view.onFocusChangeListener
                view.setOnFocusChangeListener { v, hasFocus ->
                    headerView.isSelected = hasFocus
                    iconView.isSelected = hasFocus
                    oldOnFocus?.onFocusChange(v, hasFocus)
                }
            }

        }

        class HeaderItemWithIcon(
            private val headerId: Long,
            private val headerName: String,
            val icon: Int
        ) : HeaderItem(headerId, headerName) {}
    }
}