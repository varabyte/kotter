package com.varabyte.kotter.runtime.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext

actual object KotterDispatchers {
    internal actual val Render: CoroutineDispatcher = newFixedThreadPoolContext(1, "Render Thread")
    actual val IO: CoroutineDispatcher = Dispatchers.Default
}