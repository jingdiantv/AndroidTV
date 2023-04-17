package com.kt.apps.media.mobile.utils

import android.graphics.Rect
import android.view.View
import android.widget.EditText
import androidx.annotation.CheckResult
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

val Fragment.screenWidth: Int
    get() = resources.displayMetrics.widthPixels

val Fragment.screenHeight: Int
    get() = resources.displayMetrics.heightPixels

val View.hitRectOnScreen: Rect
    get() {
        val hitRect = Rect()
        val location =  intArrayOf(0, 0)
        getHitRect(hitRect)
        getLocationOnScreen(location)
        hitRect.offset(location[0], location[1])
        return hitRect
    }

fun <T> debounce(
    waitMs: Long = 300L,
    coroutineScope: CoroutineScope,
    destinationFunction: (T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(waitMs)
            destinationFunction(param)
        }
    }
}

@CheckResult
fun EditText.textChanges(): Flow<CharSequence?> {
    return callbackFlow {
        val listener = doOnTextChanged { text, _, _, _ -> trySend(text) }
        awaitClose { removeTextChangedListener(listener) }
    }.onStart { emit(text) }
}