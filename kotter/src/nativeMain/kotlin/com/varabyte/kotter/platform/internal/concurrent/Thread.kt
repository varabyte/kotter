package com.varabyte.kotter.platform.internal.concurrent

import platform.posix.pthread_self
import platform.posix.pthread_t
import platform.posix.usleep

internal actual class ThreadId(private val pthread: pthread_t) {
    override fun equals(other: Any?): Boolean {
        return this === other || (other is ThreadId && pthread == other.pthread)
    }

    override fun hashCode() = pthread.hashCode()
    override fun toString() = pthread.toString()
}

internal actual class Thread {
    actual companion object {
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // Assertion needed when compiling on Mac
        actual fun getId(): ThreadId = com.varabyte.kotter.platform.internal.concurrent.ThreadId(pthread_self()!!)
        actual fun sleepMs(millis: Int) { usleep((millis * 1000).toUInt())}
    }
}