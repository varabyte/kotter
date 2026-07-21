package com.varabyte.kotter.foundation.terminal

import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.terminal.TerminalSize
import kotlinx.coroutines.launch

/**
 * Fields accessible within a callback triggered by [onTerminalSizeChanged].
 *
 *
 * @property terminalSize This callback provides its own copy of `terminalSize` for convenience, even though in practice
 *   a user can use [Session.terminalSize] instead. This is because sometimes a user might be extending a [RunScope]
 *   and not scoped access to the outer session in that case. However, this value and the session's value are guaranteed
 *   to be identical, so you can use whichever one is more convenient.
 */
class OnTerminalSizeChangedScope(val terminalSize: TerminalSize)

/**
 * Add a listener that gets triggered every time the terminal is resized.
 *
 * In many cases, you may prefer to just access the section's [width][MainRenderScope.width] or
 * [height][MainRenderScope.height] variables inside a section block, as that will automatically re-issue a rerender in
 * that case. But it may be useful for you to respond to the change inside a `run` block, as a way to trigger fewer
 * rerenders (if you only need to be partially responsive, e.g. to specific width thresholds but not every width) or
 * perhaps you need to make changes to an underlying data model as more or less size becomes available, and it's awkward
 * to do that inside a render loop.
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
fun RunScope.onTerminalSizeChanged(callback: OnTerminalSizeChangedScope.() -> Unit) {
    section.coroutineScope.launch {
        section.session.terminal.events.sizeChanged.collect { size -> callback(OnTerminalSizeChangedScope(size)) }
    }
}
