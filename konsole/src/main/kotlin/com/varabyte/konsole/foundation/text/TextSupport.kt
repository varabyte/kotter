package com.varabyte.konsole.foundation.text

import com.varabyte.konsole.runtime.RenderScope
import com.varabyte.konsole.runtime.internal.ansi.commands.BG_CLEAR_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.CharCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.TextCommand

fun RenderScope.textLine() {
    val activeBgColor = state.bgColorRecursive
    if (activeBgColor != null) {
        // In some terminals, if you have a background color enabled, and you append a newline, the background color
        // extends to the end of the next line. This looks awful, as if the background color is leaking, and
        // unfortunately with Konsole this would happen every time you did something like:
        // `blue(BG) { textLine("Hello") }; textLine("World")`
        // Above, a user would expect for the world "Hello" to be backgrounded by blue and for "World" to show up in
        // normal colors on the next line, but instead "Hello" is blue (good) and "World" would look correct (good) but
        // the whole line trailing AFTER "World" would be blue (really bad)
        //
        // The way we fix this here is by detecting all newlines when a background color is set, resetting state BEFORE
        // the newline, and re-enabling state afterwards.
        //
        // Note: We use "appendCommand" and not "applyCommand" intentionally because the latter would modify the current
        // state, which we don't want to do because we're trying to sneakily leave the state in its previous value.
        block.appendCommand(BG_CLEAR_COMMAND)
        applyCommand(NEWLINE_COMMAND)
        block.appendCommand(activeBgColor)
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
    run {
        val lastChar = this.block.textArea.toRawText().lastOrNull()
        if (lastChar != null && lastChar != '\n') {
            textLine()
        }
    }
    textLine()
    block()
    run {
        val lastChar = this.block.textArea.toRawText().lastOrNull()
        if (lastChar != null && lastChar != '\n') {
            textLine()
        }
    }
    textLine()
}