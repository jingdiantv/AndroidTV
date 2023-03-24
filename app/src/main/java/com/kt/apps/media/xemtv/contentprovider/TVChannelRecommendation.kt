package com.kt.apps.media.xemtv.contentprovider

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.tvprovider.media.tv.*
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.kt.apps.core.GlideApp
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.base.provider.RecommendationProvider
import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.utils.dpToPx
import com.kt.apps.media.xemtv.workers.TVRecommendationWorkers
import javax.inject.Inject

class TVChannelRecommendation @Inject constructor(
    val context: Context,
    val storage: IKeyValueStorage
) : RecommendationProvider<TVChannel>() {

    override fun sendRecommendation(item: TVChannel) {
        val recommendChannelBuilder = Channel.Builder()
        recommendChannelBuilder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
            .setDisplayName(item.tvChannelName)
            .setSearchable(true)
            .setDescription(item.tvGroup)
            .setAppLinkIntentUri(Uri.parse("xemtv://tv/channel/${item.channelId}"))
            .setAppLinkIntent(Intent().apply {
                data = Uri.parse("xemtv://tv/channel/${item.channelId}")
            })

        val uri = context.contentResolver.insert(
            TvContractCompat.Channels.CONTENT_URI,
            recommendChannelBuilder.build().toContentValues()
        )


        uri?.let {
            ContentUris.parseId(it)
        }?.let {
            TvContractCompat.requestChannelBrowsable(context, it)
            saveChannelIdForContentProvider(it, item)
        }
    }

    private fun saveChannelIdForContentProvider(
        providerChannelId: Long,
        channel: TVChannel
    ) {
        storage.save(channel.channelId, providerChannelId, Long::class.java)
        GlideApp.with(context)
            .asBitmap()
            .load(channel.logoChannel)
            .override(80.dpToPx(), 80.dpToPx())
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    ChannelLogoUtils.storeChannelLogo(
                        context,
                        providerChannelId,
                        resource
                    )
                    val updatedChannel = PreviewChannel.Builder()
                        .setInternalProviderId(channel.channelId)
                        .setLogo(resource)
                        .setAppLinkIntentUri(Uri.parse("xemtv://tv/channel/${channel.channelId}"))
                        .setAppLinkIntent(Intent().apply {
                            data = Uri.parse("xemtv://tv/channel/${channel.channelId}")
                        })
                        .setDisplayName(channel.tvChannelName)
                        .setDescription(channel.tvGroup)
                        .build()
                    Logger.d(
                        this, message = "{" +
                                "providerChannelId: $providerChannelId, " +
                                "channel: $channel" +
                                "}"
                    )
                    PreviewChannelHelper(context)
                        .updatePreviewChannel(providerChannelId, updatedChannel)

                    val existingProgramList = TVRecommendationWorkers.getPreviewPrograms(context, providerChannelId)

                    existingProgramList.forEach {
                        val existingProgram = existingProgramList.find { it.contentId == channel.channelId }
                        val programBuilder = if (existingProgram == null) {
                            PreviewProgram.Builder()
                        } else {
                            PreviewProgram.Builder(existingProgram)
                        }

                        val updatedProgram = programBuilder.setContentId(channel.channelId)
                            .setTitle(channel.tvChannelName)
                            .setType(TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
                            .setChannelId(providerChannelId)
                            .setIntentUri(Uri.parse("xemtv://tv/channel/${channel.channelId}"))
                            .build()

                        try {
                            if (existingProgram == null) {
                                PreviewChannelHelper(context).publishPreviewProgram(updatedProgram)
                            } else {
                                PreviewChannelHelper(context)
                                    .updatePreviewProgram(existingProgram.id, updatedProgram)
                            }
                        } catch (e: IllegalArgumentException) {
                            Logger.e(this@TVChannelRecommendation, exception = e)
                        }
                    }
                }


            })
    }

    companion object {
    }
}