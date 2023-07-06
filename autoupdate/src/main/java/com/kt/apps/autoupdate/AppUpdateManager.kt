package com.kt.apps.autoupdate

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kt.apps.autoupdate.di.AppUpdateComponent
import com.kt.apps.autoupdate.model.UpdateInfo
import com.kt.apps.autoupdate.model.request.AppUpdateRequest
import com.kt.apps.autoupdate.usecase.DownloadFileWorker
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.lang.ref.WeakReference
import javax.inject.Inject

class AppUpdateManager(component: AppUpdateComponent) {

    @Inject
    lateinit var repository: IUpdateRepository

    @Inject
    lateinit var context: Context

    init {
        component.inject(this)
    }

    private val disposable by lazy {
        CompositeDisposable()
    }

    private var appUpdateInfo: UpdateInfo? = null

    fun setUpdateListener() {
    }

    fun checkUpdate(request: AppUpdateRequest) {
        disposable.add(
            repository.checkUpdate(request)
                .subscribe({
                    appUpdateInfo = it
                    if (context.isSelfUpdateDelegate()) {
                        it.updateMethod.firstOrNull { method ->
                            method.type == "direct_link"
                        }?.let {
                            startDownloadApk(it)
                        }
                    }

                    when (it.priority) {
                        5 -> {
                            context.startActivity(
                                Intent().apply {
                                    data = Uri.parse("app://update/force")
                                }
                            )
                        }

                        4 -> {
                            displayUpdateApp(it)
                        }

                        else -> {
                        }
                    }
                }, {

                })
        )
    }

    private fun startDownloadApk(updateMethod: UpdateInfo.UpdateMethod) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadFileWorker>()
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "DownloadUpdate",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<DownloadFileWorker>()
                    .setInputData(
                        Data.Builder()
                            .putString(DownloadFileWorker.EXTRA_DOWNLOAD_LINK, updateMethod.link)
                            .build()
                    )
                    .build()
            )

        WorkManager.getInstance(context)
            .getWorkInfoByIdLiveData(workRequest.id)
            .observeForever {
                when (it.state) {
                    WorkInfo.State.SUCCEEDED -> {

                    }

                    WorkInfo.State.RUNNING -> {

                    }

                    WorkInfo.State.FAILED -> {

                    }

                    else -> {

                    }
                }
            }
    }

    private fun displayUpdateApp(updateInfo: UpdateInfo) {

    }

    interface UpdateListener {
        fun onStartDownload()
        fun onDownloadInProgress(progress: Float)
        fun onStartUpdate()
        fun onUpdateSuccess()
    }

    companion object {
        private var INSTANCE: WeakReference<AppUpdateManager>? = null
        fun getInstance(component: AppUpdateComponent): AppUpdateManager {
            if (INSTANCE?.get() == null) {
                INSTANCE = WeakReference(AppUpdateManager(component))
            }
            return INSTANCE!!.get()!!
        }
    }

}