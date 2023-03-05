package com.varabyte.kotter.foundation.shutdown

import com.varabyte.kotter.foundation.LiveVar
import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.Section
import com.varabyte.kotter.runtime.concurrent.createKey

internal val ShutdownHookKey = Section.Lifecycle.createKey<MutableList<() -> Unit>>()

/**
 * Add some dispose logic, tied to the current section, which should run if the user tries to exit the application via
 * Ctrl-C.
 *
 * It's critical that you keep the time of your dispose callback short -- there's a limited time after a program
 * receives an interrupt signal to when it forcefully shuts down.
 *
 * While there's no guarantee another render will happen, you can try setting `[LiveVar]` values in your dispose block,
 * as at least one more render pass will be attempted.
 */
fun RunScope.addShutdownHook(dispose: () -> Unit) {
    // Note: The value is definitely non-null -- if we're inside a run block, we know the section lifecycle is active.
    data.putOrGet(ShutdownHookKey) { mutableListOf() }!!.add(dispose)
}


