package com.varabyte.konsole.core.text

import com.varabyte.konsole.core.RenderScope
import com.varabyte.konsole.internal.ansi.commands.CharCommand
import com.varabyte.konsole.internal.ansi.commands.NewlineCommand
import com.varabyte.konsole.internal.ansi.commands.TextCommand

fun RenderScope.text(text: String) {
    applyCommand(TextCommand(text))
}

fun RenderScope.text(c: Char) {
    applyCommand(CharCommand(c))
}

fun RenderScope.textLine(text: String) {
    applyCommand(TextCommand(text))
    textLine()
}

fun RenderScope.textLine(c: Char) {
    applyCommand(CharCommand(c))
    textLine()
}

fun RenderScope.textLine() {
    applyCommand(NewlineCommand)
}

/**
 * Create a "paragraph" for text.
 *
 * This is a convenience function for wrapping a block with newlines above and below it, which is a common enough
 * pattern that it's nice to shorten it.
 */
fun RenderScope.p(block: RenderScope.() -> Unit) {
    if (lastChar != null && lastChar != '\n') { textLine() }
    textLine()
    block()
    if (lastChar != null && lastChar != '\n') { textLine() }
    textLine()
}