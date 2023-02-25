package com.varabyte.kotter.platform.concurrent
import java.lang.Thread as JvmThread

internal actual class Thread {
    actual companion object {
        actual fun getId(): Any = JvmThread.currentThread()
    }
}