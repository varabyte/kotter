package com.varabyte.kotter.foundation.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

@OptIn(ExperimentalCoroutinesApi::class)
actual object KotterDispatchers {
    internal actual val Render: CoroutineDispatcher = newSingleThreadContext("Kotter Render")
    actual val IO: CoroutineDispatcher = Dispatchers.Default
}