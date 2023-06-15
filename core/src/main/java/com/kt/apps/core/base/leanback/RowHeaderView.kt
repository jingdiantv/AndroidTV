package com.kt.apps.core.base.leanback

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.leanback.R

@SuppressLint("AppCompatCustomView")
class RowHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.rowHeaderStyle
) : TextView(context, attrs, defStyle) {
    /**
     * See
     * [TextViewCompat.setCustomSelectionActionModeCallback]
     */
    @SuppressLint("RestrictedApi")
    override fun setCustomSelectionActionModeCallback(actionModeCallback: ActionMode.Callback?) {
        super.setCustomSelectionActionModeCallback(
            TextViewCompat
                .wrapCustomSelectionActionModeCallback(this, actionModeCallback)
        )
    }

}
