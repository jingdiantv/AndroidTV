package com.kt.apps.autoupdate.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kt.apps.autoupdate.model.UpdateInfo
import com.kt.apps.autoupdate.model.request.AppUpdateRequest
import com.kt.apps.autoupdate.usecase.CheckUpdate
import com.kt.apps.autoupdate.usecase.DownloadFileWorker
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import java.io.File
import javax.inject.Inject

data class AppUpdateInteractor @Inject constructor(
    val checkUpdate: CheckUpdate
)

class AppUpdateViewModel @Inject constructor(
    private val interactor: AppUpdateInteractor
) : BaseViewModel() {

    private val _downloadFileLiveData by lazy {
        MutableLiveData<DataState<File>>()
    }

    private val _checkUpdateLiveData by lazy {
        MutableLiveData<DataState<UpdateInfo>>()
    }

    val checkUpdateLiveData: LiveData<DataState<UpdateInfo>>
        get() = _checkUpdateLiveData

    val downloadFile: LiveData<DataState<File>>
        get() = _downloadFileLiveData

    fun checkUpdate(
        context: Context,
        version: Int,
        variant: String
    ) {
        _checkUpdateLiveData.postValue(DataState.Loading())
        add(
            interactor.checkUpdate(
                AppUpdateRequest(
                    version, AppUpdateRequest.AppVariant.PLAY_STORE
                )
            ).subscribe({
                if (version < it.version && (it.updateMethod.any {
                        it.type == "direct_link"
                    })) {

                    WorkManager.getInstance(context)
                        .enqueueUniqueWork(
                            "DownloadUpdateFile",
                            ExistingWorkPolicy.KEEP,
                            OneTimeWorkRequestBuilder<DownloadFileWorker>()
                                .setInputData(
                                    Data.Builder()
                                        .build()
                                )
                                .build()
                        )


                }
                _checkUpdateLiveData.postValue(DataState.Success(it))
            }, {
                _checkUpdateLiveData.postValue(DataState.Error(it))
            })
        )
    }

    fun downloadFile(context: Context) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(DOWNLOAD_WORKER_NAME)
            .observeForever {
                Log.e("TAG", "")
            }
    }

    fun checkDownloadProgress() {
    }

    companion object {
        private const val DOWNLOAD_WORKER_NAME = "DownloadUpdateFile"
    }
}