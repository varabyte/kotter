package com.varabyte.konsole.core.internal.executor

import java.util.concurrent.Executors

val KonsoleExecutor = Executors.newSingleThreadExecutor { r ->
    Thread(r, "Konsole Thread").apply {
        isDaemon = true // Allow program to quit even if this thread is still running
    }
}
