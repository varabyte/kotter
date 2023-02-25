package com.varabyte.kotter.platform.runtime

internal actual fun onShutdown(dispose: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(Thread { dispose() })
}
