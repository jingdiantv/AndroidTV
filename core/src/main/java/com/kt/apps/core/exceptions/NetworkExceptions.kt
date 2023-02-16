package com.kt.apps.core.exceptions


class NetworkExceptions @JvmOverloads constructor(
    val errorCode: Int,
    override val message: String = "NetworkExceptions with error code: $errorCode",
    override val cause: Throwable? = null
) : Throwable(message) {
}