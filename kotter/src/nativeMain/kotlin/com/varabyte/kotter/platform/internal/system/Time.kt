package com.varabyte.kotter.platform.internal.system

import kotlin.system.getTimeMillis

internal actual fun getCurrentTimeMs() = getTimeMillis()