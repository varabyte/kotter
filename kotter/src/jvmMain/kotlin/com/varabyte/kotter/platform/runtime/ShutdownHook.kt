package com.varabyte.kotter.platform.runtime

internal actual fun onShutdown(block: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(Thread { block() })
}
