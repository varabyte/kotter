package com.varabyte.kotter.platform.internal.runtime

import kotlin.system.getTimeMillis

internal actual fun getCurrentTimeMs() = getTimeMillis()