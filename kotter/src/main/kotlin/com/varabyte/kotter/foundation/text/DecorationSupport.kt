package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.render.RenderScope

/**
 * Marks the current scope so that any text after this point will be bolded.
 */
fun RenderScope.bold() {
    applyCommand(BOLD_COMMAND)
}

/**
 * Clears a previous call to [bold].
 */
fun RenderScope.clearBold() {
    applyCommand(CLEAR_BOLD_COMMAND)
}

/**
 * Creates a new scope within which any text will be bolded.
 */
fun RenderScope.bold(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        bold()
        scopedBlock()
    }
}

/**
 * Marks the current scope so that any text after this point will be underlined.
 */
fun RenderScope.underline() {
    applyCommand(UNDERLINE_COMMAND)
}

/**
 * Clears a previous call to [underline].
 */
fun RenderScope.clearUnderline() {
    applyCommand(CLEAR_UNDERLINE_COMMAND)
}

/**
 * Creates a new scope within which any text will be underlined.
 */
fun RenderScope.underline(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        underline()
        scopedBlock()
    }
}

/**
 * Marks the current scope so that any text after this point will be struck-through.
 */
fun RenderScope.strikethrough() {
    applyCommand(STRIKETHROUGH_COMMAND)
}

/**
 * Clears a previous call to [strikethrough].
 */
fun RenderScope.clearStrikethrough() {
    applyCommand(CLEAR_STRIKETHROUGH_COMMAND)
}

/**
 * Creates a new scope within which any text will be struck-through.
 */
fun RenderScope.strikethrough(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        strikethrough()
        scopedBlock()
    }
}