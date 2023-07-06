package com.kt.apps.autoupdate.model.request

class AppUpdateRequest(
    val currentVersion: Int,
    val appVariant: AppVariant
) {

    enum class AppVariant {
        PLAY_STORE,
        MODE_BETA;
    }
}