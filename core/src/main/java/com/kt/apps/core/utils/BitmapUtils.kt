package com.kt.apps.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.*
import androidx.palette.graphics.Palette
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.kt.apps.core.GlideApp
import com.kt.apps.core.GlideRequest
import com.kt.apps.core.R
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.logging.Logger

fun Bitmap.getMainColor(): Int = Palette.from(this)
    .generate()
    .let {
        val vibrant = it.getVibrantColor(
            ContextCompat.getColor(CoreApp.getInstance(), R.color.black)
        )

        val dark = it.getDarkVibrantColor(
            ContextCompat.getColor(CoreApp.getInstance(), R.color.black)
        )
        dark
    }

fun ImageView.loadImageBitmap(
    url: String,
    @ColorInt filterColor: Int = 0,
    onResourceReady: (bitmap: Bitmap) -> Unit,
) {
    GlideApp.with(this)
        .asBitmap()
        .load(url)
        .centerInside()
        .addListener(object : RequestListener<Bitmap> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Bitmap>?,
                isFirstResource: Boolean
            ): Boolean {
                return true
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResourceReady(
                resource: Bitmap?,
                model: Any?,
                target: Target<Bitmap>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                resource?.let {
                    onResourceReady(it)
                }
                this@loadImageBitmap.setImageBitmap(resource)
                this@loadImageBitmap.setColorFilter(filterColor)
                return true
            }


        })
        .into(this)
}

fun ImageView.loadImgByUrl(url: String, scaleType: ScaleType = ScaleType.CENTER_INSIDE) {
    GlideApp.with(this)
        .load(url)
        .error(R.drawable.app_icon)
        .override(170, 120)
        .scaleType(scaleType)
        .fitCenter()
        .into(this)
}

fun ImageView.loadDrawableRes(@DrawableRes @RawRes resId: Int, scaleType: ScaleType = ScaleType.CENTER_INSIDE) {
    GlideApp.with(this)
        .load(resId)
        .error(R.drawable.app_icon)
        .scaleType(scaleType)
        .into(this)
}

fun ImageView.loadImgByDrawableIdResName(
    name: String,
    backupUrl: String? = null,
    scaleType: ScaleType = ScaleType.CENTER_INSIDE
) {
    val context = context.applicationContext
    val id = context.resources.getIdentifier(
        name.trim().removeSuffix(".png")
            .removeSuffix(".jpg")
            .removeSuffix(".webp")
            .removeSuffix(".jpeg"),
        "drawable",
        context.packageName
    )
    try {
        val drawable = ContextCompat.getDrawable(context, id)
        GlideApp.with(this)
            .load(drawable)
            .scaleType(scaleType)
            .into(this)
    } catch (e: Exception) {
        Logger.e(this, name, e)
        backupUrl?.let { url ->
            loadImgByUrl(url, scaleType)
        } ?: loadDrawableRes(R.drawable.app_icon, scaleType)
    }

}

fun <TranscodeType> GlideRequest<TranscodeType>.scaleType(scaleType: ScaleType): GlideRequest<TranscodeType> {
    return when (scaleType) {
        ScaleType.FIT_XY -> {
            this.optionalFitCenter()
        }
        ScaleType.CENTER_CROP -> {
            this.optionalCenterCrop()
        }
        else -> {
            this.optionalCenterInside()
        }
    }
}