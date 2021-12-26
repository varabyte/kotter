package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.render.RenderScope

/**
 * Clear all actively set properties for the current scope.
 *
 * While most ANSI developers are used to the RESET ALL command, this only resets the current scope, which means if
 * you do the following:
 *
 * ```
 * underline()
 * red()
 * scopedState {
 *    green()
 *    bold()
 *    clearAll()
 *    textLine("Example text")
 * }
 * ```
 *
 * then "Example text" will be underlined and red.
 */
fun RenderScope.clearAll() {
    clearColors()
    clearUnderline()
    clearBold()
    clearStrikethrough()
    clearInvert()
}