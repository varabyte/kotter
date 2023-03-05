package com.varabyte.kotter.platform.internal.concurrent

import java.lang.Thread as JvmThread

internal actual class ThreadId(private val thread: JvmThread) {
    override fun equals(other: Any?): Boolean {
        return this === other || (other is ThreadId && thread == other.thread)
    }

    override fun hashCode() = thread.hashCode()
    override fun toString() = thread.toString()
}


internal actual class Thread {
    actual companion object {
        actual fun getId(): ThreadId =
            com.varabyte.kotter.platform.internal.concurrent.ThreadId(JvmThread.currentThread())
        actual fun sleepMs(millis: Int) { JvmThread.sleep(millis.toLong()) }
    }
}