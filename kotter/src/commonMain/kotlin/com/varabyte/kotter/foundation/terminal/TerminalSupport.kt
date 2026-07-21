package com.varabyte.kotter.foundation.terminal

import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.Session
import kotlinx.coroutines.launch

/**
 * Add a listener that gets triggered every time the terminal is resized.
 *
 * In many cases, you may prefer to just access the section's [width][MainRenderScope.width] or
 * [height][MainRenderScope.height] variables inside a section block, as that will automatically re-issue a rerender in
 * that case. But it may be useful for you to respond to the change inside a `run` block, as a way to trigger fewer
 * rerenders (if you only need to be partially responsive, e.g. to specific width thresholds but not every width) or
 * perhaps you need to make major changes to an underlying data model as more or less size becomes available.
 *
 * This callback does not return any actual values, but you can query [Session.terminalSize] after it is triggered, and
 * it will have been updated.
 * ```
 * fun shouldUseWideLayout() = terminalSize.width > 50
 * var useDoubleLayout by liveVarOf(shouldUseDoubleLayout())
 * section {
 *   if (useDoubleLayout) { /* ... */ }
 * }.runUntilKeysPressed(Keys.Q) {
 *   onTerminalSizeChanged {
 *      useDoubleLayout = shouldUseDoubleLayout()
     }
 * }
 * ```
 */
fun RunScope.onTerminalSizeChanged(callback: () -> Unit) {
    section.coroutineScope.launch {
        section.session.terminal.events.sizeChanged.collect { callback() }
    }
}
