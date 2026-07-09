package com.varabyte.kotter.platform.internal.concurrent

import java.lang.Thread as JvmThread

internal actual class Thread {
    actual companion object {
        actual fun getId(): ThreadId = ThreadId(JvmThread.currentThread())

        actual fun sleepMs(millis: Int) {
            JvmThread.sleep(millis.toLong())
        }

        actual fun yield() {
            JvmThread.yield()
        }
    }
}
