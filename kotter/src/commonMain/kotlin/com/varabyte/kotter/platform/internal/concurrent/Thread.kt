package com.varabyte.kotter.platform.internal.concurrent

internal expect class ThreadId

internal expect class Thread {
    companion object {
        fun getId(): ThreadId
        fun sleepMs(millis: Int)
    }
}