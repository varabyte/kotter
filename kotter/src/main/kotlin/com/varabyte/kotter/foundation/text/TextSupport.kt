package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.internal.ansi.commands.CharCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.kotter.runtime.internal.ansi.commands.CharSequenceCommand
import com.varabyte.kotter.runtime.render.RenderScope

fun RenderScope.textLine() {
    applyCommand(NEWLINE_COMMAND)
}

fun RenderScope.text(text: CharSequence) {
    val lines = text.split('\n')
    lines.forEachIndexed { i, line ->
        applyCommand(CharSequenceCommand(line))
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

private fun RenderScope.addNewlinesIfNecessary(count: Int) {
    require(count > 0)
    // Don't add too many extra newlines when the `p` block is the first part of a section.
    var numNewlinesToAdd = count.coerceAtMost(renderer.commands.size)
    val commandsToCheck = renderer.commands.takeLast(count).toMutableList()
    while (numNewlinesToAdd > 0 && commandsToCheck.isNotEmpty()) {
        if (commandsToCheck.removeLast() === NEWLINE_COMMAND) {
            --numNewlinesToAdd
        } else break
    }

    repeat(numNewlinesToAdd) { textLine() }
}

/**
 * Create a "paragraph" for text.
 *
 * This is a convenience function for wrapping a block with newlines above and below it, which is a common enough
 * pattern that it's nice to shorten it.
 */
fun RenderScope.p(block: RenderScope.() -> Unit) {
    // Worst case, we're on the end of a previous line; in that case, add two newlines. One to add a newline at the
    // end of the last line, and another two put a line of space between this paragraph and the previous line.
    addNewlinesIfNecessary(count = 2)
    block()
    addNewlinesIfNecessary(count = 2)
}