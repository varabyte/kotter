package com.varabyte.kotter.platform.runtime

internal expect fun onShutdown(block: () -> Unit)
