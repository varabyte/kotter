package com.varabyte.kotter.platform.runtime

internal expect fun onShutdown(dispose: () -> Unit)
