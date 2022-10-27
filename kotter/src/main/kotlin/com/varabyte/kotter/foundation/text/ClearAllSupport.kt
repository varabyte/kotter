package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.render.RenderScope

/**
 * Clear all actively set properties for the current scope.
 *
 * ```
 * underline()
 * red()
 * scopedState {
 *    green()
 *    bold()
 *    clearAll()
 *    textLine("Undecorated text")
 * }
 * textLine("Underlined and red")
 * ```
 */
fun RenderScope.clearAll() {
    clearColors()
    clearUnderline()
    clearBold()
    clearStrikethrough()
    clearInvert()
}