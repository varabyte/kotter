package com.varabyte.kotter.platform.internal.system

internal expect fun onShutdown(dispose: () -> Unit)
