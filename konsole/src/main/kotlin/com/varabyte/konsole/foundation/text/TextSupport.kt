package com.varabyte.konsole.foundation.text

import com.varabyte.konsole.runtime.RenderScope
import com.varabyte.konsole.runtime.internal.ansi.commands.BG_CLEAR_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.CharCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.TextCommand

fun RenderScope.textLine() {
    applyCommand(NEWLINE_COMMAND)
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