package com.varabyte.kotter.foundation.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

actual object KotterDispatchers {
    internal actual val Render: CoroutineDispatcher = Executors.newSingleThreadExecutor() { r ->
        Thread(r, "Kotter Thread").apply {
            // This thread is never stopped so allow the program to quit even if it's running
            isDaemon = true
        }
    }.asCoroutineDispatcher()

    actual val IO: CoroutineDispatcher = Dispatchers.IO
}