package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.render.*

/**
 * Append a newline to the current section.
 */
fun RenderScope.textLine() {
    applyCommand(NEWLINE_COMMAND)
}

/**
 * Append some text to the current section.
 */
fun RenderScope.text(text: CharSequence) {
    if (text.isEmpty()) return

    val lines = text.split('\n')
    lines.forEachIndexed { i, line ->
        if (line.isNotEmpty()) applyCommand(CharSequenceCommand(line))
        if (i < lines.size - 1) {
            textLine()
        }
    }
}

/**
 * Append a character to the current section.
 */
fun RenderScope.text(c: Char) {
    if (c != '\n') {
        applyCommand(CharCommand(c))
    } else {
        textLine()
    }
}

/**
 * Append some text to the current section, followed by a newline.
 */
fun RenderScope.textLine(text: CharSequence) {
    text(text)
    textLine()
}

/**
 * Append a character to the current section, followed by a newline.
 */
fun RenderScope.textLine(c: Char) {
    text(c)
    textLine()
}

/**
 * Add [count] newlines *unless* we are already trailing previously added newlines.
 *
 * For example, if we previously added 2 newlines, and then we called `addNewlinesIfNecessary(3)`, only one newline
 * would actually get appended.
 */
internal fun RenderScope.addNewlinesIfNecessary(count: Int) {
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
