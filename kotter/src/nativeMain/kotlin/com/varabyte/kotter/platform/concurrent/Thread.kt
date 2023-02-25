package com.varabyte.kotter.platform.concurrent

import platform.posix.pthread_self
import platform.posix.usleep

internal actual class Thread {
    actual companion object {
        actual fun getId(): Any = pthread_self()
    }
}