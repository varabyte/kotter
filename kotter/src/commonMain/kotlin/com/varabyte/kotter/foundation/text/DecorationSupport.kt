package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.render.*

/**
 * Marks the current scope so that any text after this point will be bolded.
 */
fun RenderScope.bold() {
    applyCommand(DecorationCommands.Bold)
}

/**
 * Clears a previous call to [bold].
 */
fun RenderScope.clearBold() {
    applyCommand(DecorationCommands.ClearBold)
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
    applyCommand(DecorationCommands.Underline)
}

/**
 * Clears a previous call to [underline].
 */
fun RenderScope.clearUnderline() {
    applyCommand(DecorationCommands.ClearUnderline)
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
    applyCommand(DecorationCommands.Strikethrough)
}

/**
 * Clears a previous call to [strikethrough].
 */
fun RenderScope.clearStrikethrough() {
    applyCommand(DecorationCommands.ClearStrikethrough)
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
