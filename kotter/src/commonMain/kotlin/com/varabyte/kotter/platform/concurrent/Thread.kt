package com.varabyte.kotter.platform.concurrent

internal expect class Thread {
    companion object {
        fun getId(): Any
        fun sleepMs(millis: Int)
    }
}