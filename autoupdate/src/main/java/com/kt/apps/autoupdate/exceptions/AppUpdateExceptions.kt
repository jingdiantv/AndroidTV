package com.kt.apps.autoupdate.exceptions


class AppUpdateExceptions(
    val errorCode: Int,
    override val message: String = ""
) : Throwable(message) {
    companion object {
        const val ERROR_CODE_EMPTY_RESPONSE = -1001
    }
}