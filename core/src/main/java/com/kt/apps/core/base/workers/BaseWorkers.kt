package com.kt.apps.core.base.workers

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kt.apps.core.base.DataState
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

abstract class BaseWorkers<T : Any>(
    val context: Context,
    val params: WorkerParameters
) : Worker(context, params) {
    private var returnedData: DataState<T> = DataState.Loading()
    private var retryable: Boolean = true
    private val disposable by lazy {
        CompositeDisposable()
    }

    override fun doWork(): Result {
        val mStartTime = System.currentTimeMillis()
        val task = work()
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe({
                returnedData = DataState.Success(it)
            }, {
                returnedData = DataState.Error(it)
            })

        disposable.add(task)

        while (returnedData is DataState.Loading) {
            if (System.currentTimeMillis() - mStartTime > 30_000) {
                if (retryable) {
                    disposable.remove(task)
                    WorkManager.getInstance(context)
                        .enqueue(
                            OneTimeWorkRequest.Builder(this::class.java)
                                .build()
                        )
                }
            }
        }

        return when (returnedData) {
            is DataState.Success -> Result.success(
                mapToReturnedWorkData((returnedData as DataState.Success).data)
            )
            else -> Result.failure()
        }
    }

    abstract fun work(): Observable<T>

    abstract fun mapToReturnedWorkData(returnedData: T): Data
}