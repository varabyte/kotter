package com.varabyte.kotter.foundation.coroutines

import kotlinx.coroutines.CoroutineDispatcher

expect object KotterDispatchers {
    /**
     * The coroutine responsible for rendering the terminal text.
     *
     * This coroutine will be a sequential dispatcher, only running one block of work on it at a time (in other words,
     * the current render should finish before any further render is attempted; any intermediate render requests can
     * be safely dropped).
     */
    internal val Render: CoroutineDispatcher

    /**
     * The coroutine responsible for running short bursts of work, e.g. timer callbacks.
     */
    val IO: CoroutineDispatcher
}