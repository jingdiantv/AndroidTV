package com.kt.apps.autoupdate.usecase

import com.kt.apps.autoupdate.IUpdateRepository
import com.kt.apps.autoupdate.model.UpdateInfo
import com.kt.apps.autoupdate.model.request.AppUpdateRequest
import com.kt.apps.core.base.rxjava.MaybeUseCase
import io.reactivex.rxjava3.core.Maybe
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class CheckUpdate @Inject constructor(
    private val updateRepository: IUpdateRepository
) : MaybeUseCase<UpdateInfo>() {
    private var lastSyncTime = 0L
    private val isCheckingUpdate by lazy {
        AtomicBoolean()
    }

    override fun prepareExecute(params: Map<String, Any>): Maybe<UpdateInfo> {
        return updateRepository.checkUpdate(params[EXTRA_APP_UPDATE_REQUEST] as AppUpdateRequest)
            .doOnSuccess {
                isCheckingUpdate.set(false)
                cacheData = it
                lastSyncTime = System.currentTimeMillis()
            }
    }

    operator fun invoke(appUpdateRequest: AppUpdateRequest): Maybe<UpdateInfo> {
        isCheckingUpdate.set(true)
        if (cacheData != null && System.currentTimeMillis() - lastSyncTime < UPDATE_INTERVAL) {
            isCheckingUpdate.set(false)
            return Maybe.just(cacheData!!)
        }
        return execute(
            mapOf(
                EXTRA_APP_UPDATE_REQUEST to appUpdateRequest
            )
        )
    }


    companion object {
        private const val EXTRA_APP_UPDATE_REQUEST = "extra:update_request"
        private const val UPDATE_INTERVAL = 60 * 60 * 1000L
    }
}