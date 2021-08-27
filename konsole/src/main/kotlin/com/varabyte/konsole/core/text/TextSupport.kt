package com.varabyte.konsole.core.text

import com.varabyte.konsole.ansi.commands.CharCommand
import com.varabyte.konsole.ansi.commands.NewlineCommand
import com.varabyte.konsole.ansi.commands.TextCommand
import com.varabyte.konsole.core.KonsoleScope

fun KonsoleScope.text(text: String) {
    applyCommand(TextCommand(text))
}

fun KonsoleScope.text(c: Char) {
    applyCommand(CharCommand(c))
}

fun KonsoleScope.textLine(text: String) {
    applyCommand(TextCommand(text))
    textLine()
}

fun KonsoleScope.textLine(c: Char) {
    applyCommand(CharCommand(c))
    textLine()
}

fun KonsoleScope.textLine() {
    applyCommand(NewlineCommand)
}

/**
 * Create a "paragraph" for text.
 *
 * This is a convenience function for wrapping a block with newlines above and below it, which is a common enough
 * pattern that it's nice to shorten it.
 */
fun KonsoleScope.p(block: KonsoleScope.() -> Unit) {
    if (lastChar != null && lastChar != '\n') { textLine() }
    textLine()
    block()
    if (lastChar != null && lastChar != '\n') { textLine() }
    textLine()
}