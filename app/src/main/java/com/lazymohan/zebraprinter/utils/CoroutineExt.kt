package com.lazymohan.zebraprinter.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

fun CoroutineScope.launchWithHandler(
    dispatcher: CoroutineDispatcher,
    onCoroutineException: (Throwable) -> Unit,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val ceh = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            onCoroutineException(throwable)
        }
    }
    return launch(dispatcher + ceh) {
        block()
    }
}

/*
 * This is a CoroutineExceptionHandler that does nothing. It is useful when you want to ignore
 * exceptions in a coroutine.
 *
 */
val EMPTY_EXCEPTION_HANDLER = CoroutineExceptionHandler { _, _ -> }