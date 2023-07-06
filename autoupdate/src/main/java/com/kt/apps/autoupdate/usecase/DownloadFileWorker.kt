package com.kt.apps.autoupdate.usecase

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import com.kt.apps.autoupdate.BuildConfig
import com.kt.apps.autoupdate.calculateMD5
import com.kt.apps.autoupdate.checkSum
import com.kt.apps.autoupdate.exceptions.AppUpdateExceptions
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadFileWorker(
    context: Context,
    workerParams: WorkerParameters
) : RxWorker(context, workerParams) {

    override fun getBackgroundScheduler(): Scheduler {
        return Schedulers.io()
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10_000, TimeUnit.MILLISECONDS)
            .writeTimeout(30_000, TimeUnit.MILLISECONDS)
            .build()
    }

    override fun createWork(): Single<Result> {
        val md5 = inputData.getString(EXTRA_CHECK_SUM_FILE).takeIf {
            !it.isNullOrBlank()
        }
            ?: return Single.error(Throwable(""))
        val apkFile = File(applicationContext.filesDir, "$APK_FOLDER_NAME/$fileName")

        if (apkFile.exists()) {
            if (apkFile.checkSum(md5)) {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG, "Apk fill exists: {" +
                                "path: ${apkFile.path}," +
                                "checkSum: $md5" +
                                "}"
                    )
                }
                return Single.just(Result.success())
            }
        }

        val downloadLink = inputData.getString(EXTRA_DOWNLOAD_LINK).takeIf {
            !it.isNullOrBlank()
        } ?: return Single.error(Throwable(""))


        return downloadFile(downloadLink, md5)
            .retry { times, throwable ->
                return@retry times < 3 &&
                        (throwable is AppUpdateExceptions && throwable.errorCode > 0)
            }
            .map {
                Result.success(
                    Data.Builder()
                        .putString(EXTRA_APK_FILE_PATH, it.absolutePath)
                        .build()
                )
            }
    }

    private fun downloadFile(downloadLink: String, md5File: String) = Single.create<File> {
        val response = client.newCall(
            Request.Builder()
                .url(downloadLink)
                .build()
        ).execute()

        val apkFile = File(applicationContext.filesDir, "$APK_FOLDER_NAME/$fileName")
        val fileLength = inputData.getLong(EXTRA_FILE_LENGTH, 17 * MB)

        if (it.isDisposed) {
            return@create
        }

        if (response.code in 200..299) {
            val body = response.body
            val source = body.source()
            val bufferedSink: BufferedSink = apkFile.sink().buffer()
            val buffer = bufferedSink.buffer
            var totalBytesRead: Long = 0
            var read: Long
            var progress = 0f
            this.setProgressAsync(
                Data.Builder()
                    .putFloat(EXTRA_PROGRESS, progress)
                    .build()
            )
            while (source.read(buffer, BUFFER_SIZE.toLong()).also { read = it } != -1L) {
                bufferedSink.emit()
                totalBytesRead += read
                progress = totalBytesRead.toFloat() / fileLength
                setProgressAsync(
                    Data.Builder()
                        .putFloat(EXTRA_PROGRESS, progress)
                        .build()
                )
            }
            bufferedSink.flush()
            bufferedSink.close()
            source.close()
            response.closeQuietly()
            if (apkFile.checkSum(md5File)) {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG, "Download apk file success: {" +
                                "path: ${apkFile.path}," +
                                "checkSum: $md5File" +
                                "}"
                    )
                }
                it.onSuccess(apkFile)
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG, "Download apk file fail: {" +
                                "path: ${apkFile.path}," +
                                "md5File: ${apkFile.calculateMD5()}," +
                                "checkSum: $md5File" +
                                "}"
                    )
                }
                it.onError(AppUpdateExceptions(1))
            }
        } else {
            response.closeQuietly()
            it.onError(AppUpdateExceptions(1))
        }
    }

    companion object {
        const val MB = 1024 * 1024L
        const val BUFFER_SIZE = 8 * 1024
        const val fileName = "newVersion"
        const val APK_FOLDER_NAME = "Update"
        const val TAG = "AppUpdateWorker"
        const val EXTRA_DOWNLOAD_LINK = "extra:download_link"
        const val EXTRA_CHECK_SUM_FILE = "extra:check_sum"
        const val EXTRA_PROGRESS = "extra:progress"
        const val EXTRA_FILE_LENGTH = "extra:file_length"
        const val EXTRA_APK_FILE_PATH = "extra:apk_file_path"

    }

}