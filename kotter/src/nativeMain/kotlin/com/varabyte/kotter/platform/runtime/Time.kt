package com.varabyte.kotter.platform.runtime

import kotlin.system.getTimeMillis

internal actual fun getCurrentTimeMs() = getTimeMillis()