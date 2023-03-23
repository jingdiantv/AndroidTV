package com.kt.skeleton

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class AdapterSkeleton(
    private val layoutRes: Int,
    private val childAdapterSkeleton: AdapterSkeleton? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var itemCount: Int? = null

    constructor(layoutRes: Int, itemCount: Int?) : this(layoutRes) {
        this.itemCount = itemCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val childView = view.getChildAt(i)
                if (childView is RecyclerView) {
                    childView.adapter = childAdapterSkeleton
                    childView.runLayoutAnimation()
                    break
                }
            }
        }
        return ViewHolder(view, layoutRes)
    }


    override fun getItemCount(): Int = if (itemCount != null) itemCount!! else  10

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).onBind(position)
    }

    class ViewHolder(view: View, val layoutRes: Int) : RecyclerView.ViewHolder(view) {
        companion object {
            public const val DELAY_TIME = 100
        }

        private val topMargin by lazy {
            (itemView.resources.displayMetrics.scaledDensity * 20).toInt()
        }
        private val margin by lazy {
            (itemView.resources.displayMetrics.scaledDensity * 5).toInt()
        }

        init {
            (itemView as ViewGroup).runChildAnimDrawable()
        }

        fun onBind(position: Int) {
            val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams
            val horizontal = (20 * itemView.context.resources.displayMetrics.scaledDensity).toInt()
            val top = (20 * itemView.context.resources.displayMetrics.scaledDensity).toInt()
            val bottom = (10 * itemView.context.resources.displayMetrics.scaledDensity).toInt()

//            when (layoutRes) {
//                else -> {
//                    Handler().postDelayed({
//                        (itemView as ViewGroup).forEach {
//                            it.runFadeInAnimation()
//                        }
//                    }, position * DELAY_TIME.toLong())
//                }
//            }
        }

    }
}