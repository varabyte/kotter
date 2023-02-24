package com.varabyte.kotter.platform.runtime

import kotlinx.cinterop.staticCFunction
import platform.posix.SIGINT
import platform.posix.signal

internal actual fun onShutdown(block: () -> Unit) {
    signal(SIGINT, staticCFunction<Int, Unit> {
//        block()
        // DO NOT SUBMIT
    })
}
