package com.kt.apps.core.exceptions

import com.kt.apps.core.ErrorCode
import com.kt.apps.xembongda.exceptions.MyException

class ConnectionTimeOut(override val message: String, override val cause: Throwable? = null) :
    MyException(ErrorCode.CONNECT_TIMEOUT, message) {
}