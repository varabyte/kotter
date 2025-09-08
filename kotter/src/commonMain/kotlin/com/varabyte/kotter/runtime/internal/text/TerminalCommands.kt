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
                if (command === TextCommands.Newline) {
                    lengths.add(0)
                } else {
                    lengths[lengths.lastIndex] += command.text.length
                }
            }
            lengths
        }
    }

private fun List<TerminalCommand>.insertCommandAtLineBreaks(commandToInsert: TerminalCommand, width: Int): List<TerminalCommand> {
    val commands = this

    return buildList {
        var currLineWidth = 0
        for (command in commands) {
            if (command is TextCommand) {
                if (command === TextCommands.Newline) {
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
                            add(TextCommands.Text(buffer.toString()))
                            add(commandToInsert)
                            buffer.clear()
                            currLineWidth = 0
                        }
                        buffer.append(nextChar)
                        currLineWidth += charWidth
                    }
                    if (buffer.isNotEmpty()) {
                        add(TextCommands.Text(buffer.toString()))
                    }
                }
            } else {
                add(command)
            }
        }
    }
}

/**
 * Manually insert implicit newlines where lines should break.
 *
 * These mark where newlines will get added but don't actually cause newlines to occur. This can be useful for being
 * able to keep track of how many lines need to be repainted, taking terminal auto-newline wrapping into account.
 */
internal fun List<TerminalCommand>.withImplicitNewlines(width: Int): List<TerminalCommand> {
    return insertCommandAtLineBreaks(TextCommands.ImplicitNewline, width)
}

/**
 * Manually insert newlines into text that would go over some width.
 *
 * This can be useful for rendering text into constrained spaces, e.g. when rendering into a grid cell.
 */
internal fun List<TerminalCommand>.withExplicitNewlines(width: Int): List<TerminalCommand> {
    return insertCommandAtLineBreaks(TextCommands.Newline, width)
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
