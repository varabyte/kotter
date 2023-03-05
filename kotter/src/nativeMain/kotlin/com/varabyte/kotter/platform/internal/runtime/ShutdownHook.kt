package com.varabyte.kotter.platform.internal.runtime

import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.posix.SIGINT
import platform.posix.signal
import platform.posix.usleep
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

// Could be there's another way to do this, but the native signal handler is really restrictive.
// Instead, I spawn up a background coroutine which polls a static value which gets set by CTRL-C.
// Then, it sets another static value which the signal handler checks for.
private const val PULSE_MS = 100

private var interrupted = false
private var shutdownCountdown = 10 // Give the dispose coroutine a chance to wake up, run, and finish

internal actual fun onShutdown(dispose: () -> Unit) {
    CoroutineScope(Dispatchers.Default).launch {
        while(!interrupted) {
            delay(PULSE_MS.milliseconds)
        }
        dispose()
        shutdownCountdown = 0
    }

    signal(SIGINT, staticCFunction<Int, Unit> {
        if (interrupted) return@staticCFunction // Already called previously

        interrupted = true
        while (shutdownCountdown > 0) {
            usleep((PULSE_MS * 1000).toUInt())
            shutdownCountdown--
        }

        exitProcess(130) // 130 == 128+2, where 2 == SIGINT
    })
}
