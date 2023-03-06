package com.kt.apps.xembongda.exceptions

import com.kt.apps.core.ErrorCode
import com.kt.apps.core.exceptions.ConnectionTimeOut
import java.net.SocketTimeoutException

abstract class MyException(
    val code: Int, override val message: String?,
    override val cause: Throwable? = null
) : Throwable(message, cause) {

}

fun Throwable.mapToMyException(msg: String? = null): MyException {
    return when (this) {
        is SocketTimeoutException -> ConnectionTimeOut(
            msg ?: "Không thể kết nối tới server, vui lòng thử link khác nhé"
        )
        else -> object : MyException(ErrorCode.UN_EXPECTED_ERROR, this.message, this.cause) {}
    }
}