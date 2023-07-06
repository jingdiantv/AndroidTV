package com.kt.apps.autoupdate.di

import com.kt.apps.autoupdate.AppUpdateManager
import com.kt.apps.autoupdate.IUpdateRepository
import com.kt.apps.autoupdate.usecase.CheckUpdate
import com.kt.apps.core.di.CoreComponents
import dagger.Component

@Component(
    modules = [AppUpdateModule::class],
    dependencies = [CoreComponents::class]
)
@AppUpdateScope
interface AppUpdateComponent {
    fun inject(appUpdateManager: AppUpdateManager)

    fun appUpdateRepository(): IUpdateRepository
    fun checkUpdate(): CheckUpdate
}