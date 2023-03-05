package com.varabyte.kotter.platform.internal.runtime

internal actual fun onShutdown(dispose: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(Thread { dispose() })
}
