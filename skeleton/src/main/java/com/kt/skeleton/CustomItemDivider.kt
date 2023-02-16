package com.kt.skeleton

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

open class CustomItemDivider(
    val context: Context,
    val orientation: Int = LinearLayoutManager.VERTICAL,
    private val margin: Float = 0f,
    private val useDefault: Boolean = false,
    private val customColor: Int = Color.parseColor("#20FFFFFF")
) : DividerItemDecoration(context, orientation) {
    private var customDrawable: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.line_divider)!!

    override fun setDrawable(drawable: Drawable) {
        super.setDrawable(drawable)
        customDrawable = drawable
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (orientation == VERTICAL) {
            c.save()
            val leftMargin = convertDpToPixel(margin, context)
            val right: Int
            val left: Int
            if (parent.clipToPadding) {
                left = (parent.paddingLeft + leftMargin).toInt()
                right = parent.width - parent.paddingRight
                c.clipRect(
                    left, parent.paddingTop, right,
                    parent.height - parent.paddingBottom
                )
            } else {
                left = leftMargin.toInt()
                right = parent.width
            }
            val mFieldBounds = DividerItemDecoration::class.java.getDeclaredField("mBounds")
            val mDrawable = DividerItemDecoration::class.java.getDeclaredField("mDivider")
            mDrawable.isAccessible = true
            mFieldBounds.isAccessible = true
            val mBounds = mFieldBounds.get(this) as Rect
            val mDivider =
                if (useDefault) mDrawable.get(this) as Drawable else customDrawable!!.apply {
                    when (this) {
                        is ShapeDrawable -> {
                            this.paint.color = customColor
                        }
                        is GradientDrawable -> {
                            this.setColor(customColor)
                        }
                        is ColorDrawable -> {
                            this.color = customColor
                        }
                    }
                }
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                parent.getDecoratedBoundsWithMargins(child, mBounds)
                val bottom = mBounds.bottom + child.translationY.roundToInt()
                val top = bottom - convertDpToPixel(1f, context).roundToInt()
                mDivider.setBounds(left, top, right, bottom)
                mDivider.draw(c)
            }
            c.restore()
        } else {
            super.onDraw(c, parent, state)
        }
    }


}