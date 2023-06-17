package com.varabyte.kotter.runtime.internal.text

import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.kotter.runtime.internal.ansi.commands.TextCommand

/**
 * Return the length of each line of text a list of [TerminalCommand]s would generate if rendered.
 */
internal val List<TerminalCommand>.lineLengths: List<Int>
    get() {
        return this.fold(mutableListOf(0)) { lengths, command ->
            if (command is TextCommand) {
                if (command === NEWLINE_COMMAND) {
                    lengths.add(0)
                } else {
                    lengths[lengths.lastIndex] += command.text.length
                }
            }
            lengths
        }
    }

/**
 * Manually insert newlines commands that would normally be auto-added by the terminal due to the limited window width.
 *
 * While this may seem redundant, as the terminal would have added the newlines for us anyway, it can be useful for us
 * to know where the newlines are explicitly which we can use when repainting the screen.
 */
internal fun List<TerminalCommand>.withAutoNewlines(width: Int): List<TerminalCommand> {
    val commands = this
    return buildList {
        var currWidth = 0
        for (command in commands) {
            if (command is TextCommand) {
                if (command === NEWLINE_COMMAND) {
                    currWidth = 0
                    add(command)
                } else {
                    var remainingText = command.text
                    while (remainingText.isNotEmpty()) {
                        val text = remainingText.take(width - currWidth)
                        remainingText = remainingText.drop(text.length)
                        add(TextCommand(text))
                        currWidth += text.length
                        if (currWidth == width) {
                            currWidth = 0
                            if (remainingText.isNotEmpty()) add(NEWLINE_COMMAND)
                        }
                    }
                }
            } else {
                add(command)
            }
        }
    }
}

/**
 * Convert a list of [TerminalCommand]s to a raw string (including ANSI escape codes) which can then be passed to a
 * console output stream.
 */
internal fun List<TerminalCommand>.toText(height: Int = Int.MAX_VALUE): String {
    val commands = this
    if (height == Int.MAX_VALUE) {
        return commands.joinToString("") { it.text }
    } else {
        val targetNumLines = commands.count { it === NEWLINE_COMMAND } + 1
        // Note: We skip rendering *text* but NOT commands, which we would still like to apply as they may
        // affect following lines that do get rendered.
        var numLinesToSkipText = (targetNumLines - height).coerceAtLeast(0)

        return buildString {
            for (command in commands) {
                if (numLinesToSkipText > 0 && command is TextCommand) {
                    if (command === NEWLINE_COMMAND) numLinesToSkipText--
                } else {
                    append(command.text)
                }
            }
        }
    }
}