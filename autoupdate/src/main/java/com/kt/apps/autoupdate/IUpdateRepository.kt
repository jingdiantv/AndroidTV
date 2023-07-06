package com.kt.apps.autoupdate

import com.kt.apps.autoupdate.model.UpdateInfo
import com.kt.apps.autoupdate.model.request.AppUpdateRequest
import io.reactivex.rxjava3.core.Maybe

interface IUpdateRepository {
    fun checkUpdate(
        appUpdateRequest: AppUpdateRequest
    ): Maybe<UpdateInfo>
}