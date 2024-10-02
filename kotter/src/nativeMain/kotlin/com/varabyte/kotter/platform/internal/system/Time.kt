package com.varabyte.kotter.platform.internal.system

import kotlin.time.TimeSource

private val timeMark by lazy { TimeSource.Monotonic.markNow() }

internal actual fun getCurrentTimeMs() = timeMark.elapsedNow().inWholeMilliseconds