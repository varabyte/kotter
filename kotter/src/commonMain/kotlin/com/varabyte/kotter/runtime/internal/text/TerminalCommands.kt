package com.varabyte.kotter.runtime.internal.text

import com.varabyte.kotter.runtime.internal.*
import com.varabyte.kotter.runtime.internal.ansi.commands.*

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
internal fun List<TerminalCommand>.withImplicitNewlines(width: Int): List<TerminalCommand> {
    val commands = this
    return buildList {
        var currLineWidth = 0
        for (command in commands) {
            if (command is TextCommand) {
                if (command === NEWLINE_COMMAND) {
                    currLineWidth = 0
                    add(command)
                } else {
                    val buffer = StringBuilder()
                    var currIndex = 0
                    while (currIndex <= command.text.lastIndex) {
                        val nextChar = command.text[currIndex++]
                        val charWidth = nextChar.toRenderWidth()
                        val remainingWidth = width - currLineWidth
                        if (charWidth > remainingWidth) {
                            add(TextCommand(buffer.toString()))
                            add(IMPLICIT_NEWLINE_COMMAND)
                            buffer.clear()
                            currLineWidth = 0
                        }
                        buffer.append(nextChar)
                        currLineWidth += charWidth
                    }
                    if (buffer.isNotEmpty()) {
                        add(TextCommand(buffer.toString()))
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
        val targetNumLines = commands.count { it is NewlineCommand } + 1
        // Note: We skip rendering *text* but NOT commands, which we would still like to apply as they may
        // affect following lines that do get rendered.
        var numLinesToSkipText = (targetNumLines - height).coerceAtLeast(0)

        return buildString {
            for (command in commands) {
                if (numLinesToSkipText > 0 && command is TextCommand) {
                    if (command is NewlineCommand) numLinesToSkipText--
                } else {
                    append(command.text)
                }
            }
        }
    }
}
