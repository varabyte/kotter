package com.varabyte.konsole.core.internal.executor

import java.util.concurrent.Executors

val KonsoleExecutor = Executors.newSingleThreadExecutor { r ->
    Thread(r, "Konsole Thread").apply {
        // This thread is never stopped so allow the program to quit even if it's running
        isDaemon = true
    }
}
