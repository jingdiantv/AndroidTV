package com.kt.apps.core.base.dialog

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.kt.apps.core.R

class ProgressDialog(context: Context) : AlertDialog(context) {

    private val alertDialog by lazy {
        AlertDialog.Builder(context)
            .setView(R.layout.layout_progress_dialog)
            .create()
    }

    override fun create() {
        super.create()

    }

    override fun setView(view: View?) {
        super.setView(view)
    }

}