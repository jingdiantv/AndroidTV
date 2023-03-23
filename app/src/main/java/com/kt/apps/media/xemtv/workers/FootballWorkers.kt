package com.kt.apps.media.xemtv.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kt.apps.football.model.FootballDataSourceFrom
import com.kt.apps.football.usecase.GetListFootballMatch
import io.reactivex.rxjava3.disposables.CompositeDisposable

class FootballWorkers(
    context: Context,
    params: WorkerParameters,
    val getListFootballMatch: GetListFootballMatch
) : Worker(context, params) {
    private val lock by lazy {

    }

    private val disposable by lazy {
        CompositeDisposable()
    }

    override fun doWork(): Result {

        disposable.add(
            getListFootballMatch.invoke(FootballDataSourceFrom.Phut91)
                .subscribe({

                }, {

                })
        )

        return Result.success()

    }
}