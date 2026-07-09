package com.varabyte.kotter.platform.internal.concurrent

import kotlin.jvm.JvmInline

// Inline to avoid many tiny memory allocations in our lock code that potentially checks thread states a lot!
@JvmInline
internal value class ThreadId(private val osThread: Any) {
    override fun toString() = osThread.toString()
}

internal expect class Thread {
    companion object {
        fun getId(): ThreadId
        fun sleepMs(millis: Int)
        fun yield()
    }
}