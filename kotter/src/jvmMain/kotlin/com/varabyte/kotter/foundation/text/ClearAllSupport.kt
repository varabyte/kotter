package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.render.RenderScope

/**
 * Clear all actively set text effects.
 *
 * For example:
 *
 * ```
 * underline()
 * red()
 * scopedState {
 *    green()
 *    bold()
 *    textLine("Green, bolded, and underlined")
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