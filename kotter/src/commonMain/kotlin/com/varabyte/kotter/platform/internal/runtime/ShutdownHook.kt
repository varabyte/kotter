package com.varabyte.kotter.platform.internal.runtime

internal expect fun onShutdown(dispose: () -> Unit)
