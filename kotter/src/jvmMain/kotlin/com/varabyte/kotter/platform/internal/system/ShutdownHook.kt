package com.varabyte.kotter.platform.internal.system

internal actual fun onShutdown(dispose: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(Thread { dispose() })
}
