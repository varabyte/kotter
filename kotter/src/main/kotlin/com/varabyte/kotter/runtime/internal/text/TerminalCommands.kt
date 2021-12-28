package com.varabyte.kotter.runtime.internal.text

import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.TextCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND

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
 * Return the total number of lines a list of [TerminalCommand]s would generate if rendered.
 *
 * [width] An optional parameter which, if set, means the terminal will count as if a newline was auto-added for
 *   wrapping at that column.
 */
internal fun List<TerminalCommand>.numLines(width: Int = Int.MAX_VALUE): Int {
    val lineLengths = lineLengths
    return lineLengths.size +
            // The line gets an implicit newline once it goes ONE over the terminal width - or in other
            // words, a 20 character line fits perfectly in a 20 column terminal, so don't treat that case
            // as an extra newline until we hit 21 characters
            lineLengths.sumOf { len -> (len - 1) / width }
}

/**
 * Convert a list of [TerminalCommand]s to a raw string (including ANSI escape codes) which can then be passed to a
 * console output stream.
 */
internal fun List<TerminalCommand>.toRawText(): String {
    return this.joinToString("") { it.text }
}