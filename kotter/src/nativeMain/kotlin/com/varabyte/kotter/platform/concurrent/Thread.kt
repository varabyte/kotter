package com.varabyte.kotter.platform.concurrent

import platform.posix.pthread_self
import platform.posix.usleep

internal actual class Thread {
    actual companion object {
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // Assertion needed when compiling on Mac
        actual fun getId(): Any = pthread_self()!!
        actual fun sleepMs(millis: Int) { usleep((millis * 1000).toUInt())}
    }
}