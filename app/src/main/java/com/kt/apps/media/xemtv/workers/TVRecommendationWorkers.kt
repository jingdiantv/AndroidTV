package com.kt.apps.media.xemtv.workers

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.StrictMode
import android.util.Log
import androidx.tvprovider.media.tv.*
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kt.apps.core.Constants
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.dto.TVChannelEntity
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.media.xemtv.App
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.workers.factory.ChildWorkerFactory
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Calendar

class TVRecommendationWorkers(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val disposable by lazy {
        CompositeDisposable()
    }
    private val tvChannelDAO by lazy {
        RoomDataBase.getInstance(context)
            .tvChannelEntityDao()
    }

    override fun doWork(): Result {
        return try {
            insertOrUpdatePreviewChannel()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    fun resourceUri(resources: Resources, id: Int): Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(resources.getResourcePackageName(id))
        .appendPath(resources.getResourceTypeName(id))
        .appendPath(resources.getResourceEntryName(id))
        .build()

    @SuppressLint("RestrictedApi")
    @Synchronized
    fun insertOrUpdatePreviewChannel() {
        val tvChannelUseCase = (context.applicationContext as App)
            .tvComponents
            .getListTVChannel()

        disposable.add(
            tvChannelUseCase.invoke(false)
                .observeOn(Schedulers.io())
                .flatMapCompletable { channelList ->
                    Logger.d(this, "Thread", Thread.currentThread().name)
                    val allChannels: List<PreviewChannel> = try {
                        PreviewChannelHelper(context).allChannels
                    } catch (exc: IllegalArgumentException) {
                        listOf()
                    }

                    PreviewChannelHelper(context).allChannels.forEach {
                        Logger.d(this, message = "$it")
                    }

                    val existingChannel = allChannels.find { it.internalProviderId == "tvChannelIMedia" }

                    Logger.d(this, message = "existingChannel: $existingChannel")

                    val channelBuilder = if (existingChannel == null) {
                        PreviewChannel.Builder()
                    } else {
                        PreviewChannel.Builder(existingChannel)
                    }

                    val channelUpdate = channelBuilder.setDisplayName("XemTV")
                        .setLogo(resourceUri(App.get().resources, R.mipmap.ic_launcher))
                        .setDescription("iMedia")
                        .setInternalProviderId("tvChannelIMedia")
                        .setAppLinkIntentUri(Uri.parse("xemtv://tv/dashboard"))
                        .build()

                    val channelProviderId = if (existingChannel == null) {
                        PreviewChannelHelper(context)
                            .publishChannel(channelUpdate)
                    } else {
                        PreviewChannelHelper(context)
                            .updatePreviewChannel(
                                existingChannel.id,
                                channelUpdate
                            )
                        existingChannel.id
                    }

                    val uri = TvContractCompat.buildPreviewProgramsUriForChannel(channelProviderId)
                    Logger.d(this, "Uri", uri.toString())

                    val channelEntity = channelList.map(mapToEntity(channelProviderId))

                    if (allChannels.none { it.isBrowsable }) {
                        TvContractCompat.requestChannelBrowsable(context, channelProviderId)
                    }

                    val existingProgramList = getPreviewPrograms(context, channelProviderId)

                    channelEntity.forEach { tvChannel ->

                        val existingProgram = existingProgramList.find { it.contentId == tvChannel.channelId }
                        Logger.d(this, message = "existingProgram: $existingProgram")

                        val programBuilder = if (existingProgram == null) {
                            PreviewProgram.Builder()
                        } else {
                            PreviewProgram.Builder(existingProgram)
                        }

                        val updatedProgram = programBuilder.setContentId(tvChannel.channelId)
                            .setLogoUri(tvChannel.logoChannel)
                            .setTitle(tvChannel.tvChannelName)
                            .setType(TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
                            .setChannelId(channelProviderId)
                            .setThumbnailUri(tvChannel.logoChannel)
                            .setBrowsable(true)
                            .setDurationMillis(0)
                            .setPosterArtUri(tvChannel.logoChannel)
                            .setReleaseDate(Calendar.getInstance().time)
                            .setSearchable(true)
                            .setPosterArtUri(tvChannel.logoChannel)
                            .setIntentUri(Uri.parse("xemtv://tv/channel/${tvChannel.channelId}"))
                            .build()

                        try {
                            Logger.d(this, message = "Update program")
                            if (existingProgram == null) {
                                PreviewChannelHelper(context)
                                    .publishPreviewProgram(updatedProgram)
                            } else {
                                PreviewChannelHelper(context)
                                    .updatePreviewProgram(existingProgram.id, updatedProgram)
                            }
                        } catch (e: IllegalArgumentException) {
                            Logger.e(this@TVRecommendationWorkers, exception = e)
                        }

                    }
                    tvChannelDAO.insert(channelEntity)
                }
                .subscribe({
                    Logger.d(this@TVRecommendationWorkers, message = "Insert preview channel success")
                }, {
                    Logger.e(this@TVRecommendationWorkers, exception = it)
                })

        )
    }

    private fun mapToEntity(providerId: Long) = { channel: TVChannel ->
        val logoUri = try {
            val id = context.resources.getIdentifier(
                Constants.mapChannel[channel.tvChannelName]!!
                    .removeSuffix(".png")
                    .removeSuffix(".jpg"),
                "drawable",
                context.packageName
            )

            if (id == 0 || id == -1) {
                throw Exception("Not found local resource for channel: $channel")
            }
            resourceUri(context.resources, id)
        } catch (e: Exception) {
            Uri.parse(channel.logoChannel)
        }

        TVChannelEntity(
            tvChannelName = channel.tvChannelName,
            channelId = channel.channelId,
            sourceFrom = channel.sourceFrom,
            logoChannel = logoUri,
            tvGroup = channel.tvGroup,
            tvChannelWebDetailPage = channel.tvChannelWebDetailPage,
            channelPreviewProviderId = providerId
        )
    }

    companion object {

        @SuppressLint("RestrictedApi")
        fun getPreviewPrograms(context: Context, channelId: Long? = null): List<PreviewProgram> {
            val programs: MutableList<PreviewProgram> = mutableListOf()

            try {
                val cursor = context.contentResolver.query(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    PreviewProgram.PROJECTION,
                    null,
                    null,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val program = PreviewProgram.fromCursor(cursor)
                        if (channelId == null || channelId == program.channelId) {
                            programs.add(program)
                        }
                    } while (cursor.moveToNext())
                }
                cursor?.close()

            } catch (exc: IllegalArgumentException) {
                Logger.e(this, "Error retrieving preview programs", exc)
            }

            return programs
        }
    }

    class Factory : ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): ListenableWorker {
            return TVRecommendationWorkers(appContext, params)
        }
    }
}