package com.varabyte.kotter.platform.concurrent

internal expect class ThreadId

internal expect class Thread {
    companion object {
        fun getId(): ThreadId
        fun sleepMs(millis: Int)
    }
}