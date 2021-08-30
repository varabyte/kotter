package com.varabyte.konsole.foundation.text

import com.varabyte.konsole.runtime.RenderScope
import com.varabyte.konsole.runtime.internal.ansi.commands.*
import com.varabyte.konsole.runtime.internal.ansi.commands.BG_CLEAR_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.CharCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.RESET_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.TextCommand

fun RenderScope.textLine() {
    val activeBgColor = state.bgColorRecursive
    if (activeBgColor != null) {
        // In some terminals, if you have a background color enabled, and you append a newline, the background color
        // extends to the end of the next line. This looks awful, as if the background color is leaking, and
        // unfortunately with Konsole this would happen every time you did something like:
        // `blue(BG) { textLine("Hello") }`
        // Above, a user would expect for the world "Hello" to be covered in blue and for the next word to be ready to
        // start printing on the next line in default colors, but instead what happens is the word itself is blue AND
        // the next line too.
        //
        // The way we fix this here is by detecting all newlines WHEN a background color is set, resetting state BEFORE
        // the newline, and re-enabling state afterwards.
        scopedState {
            // Use "appendCommand" and not "applyCommand" because the latter will actually modify the current state
            // value, which we don't want to do because we're trying to leave the state in its previous value.
            block.appendCommand(BG_CLEAR_COMMAND)
            applyCommand(NEWLINE_COMMAND)
            block.appendCommand(activeBgColor)
        }
    }
    else {
        applyCommand(NEWLINE_COMMAND)
    }
}

fun RenderScope.text(text: CharSequence) {
    val lines = text.split('\n')
    lines.forEachIndexed { i, line ->
        applyCommand(TextCommand(line))
        if (i < lines.size - 1) {
            textLine()
        }
    }
}

fun RenderScope.text(c: Char) {
    if (c != '\n') {
        applyCommand(CharCommand(c))
    }
    else {
        textLine()
    }
}

fun RenderScope.textLine(text: CharSequence) {
    text(text)
    textLine()
}

fun RenderScope.textLine(c: Char) {
    text(c)
    textLine()
}

/**
 * Create a "paragraph" for text.
 *
 * This is a convenience function for wrapping a block with newlines above and below it, which is a common enough
 * pattern that it's nice to shorten it.
 */
fun RenderScope.p(block: RenderScope.() -> Unit) {
    val lastChar = this.block.textArea.lastChar
    if (lastChar != null && lastChar != '\n') { textLine() }
    textLine()
    block()
    if (lastChar != null && lastChar != '\n') { textLine() }
    textLine()
}