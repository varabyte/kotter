package com.varabyte.kotter.runtime.internal.text

import com.varabyte.kotter.runtime.internal.*
import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.terminal.TextMetrics

private fun List<TerminalCommand>.mapLinesIntoNumericValue(calcText: (String) -> Int): List<Int> {
    return this.fold(mutableListOf(0)) { lengths, command ->
        if (command is TextCommand) {
            if (command is NewlineCommand) {
                lengths.add(0)
            } else {
                lengths[lengths.lastIndex] += calcText(command.text)
            }
        }
        lengths
    }
}

/**
 * Return the length of each line of text a list of [TerminalCommand]s would generate if rendered.
 */
internal val List<TerminalCommand>.lineLengths: List<Int>
    get() {
        return this.mapLinesIntoNumericValue { text -> text.length }
    }

/**
 * Return the render width of each line of text a list of [TerminalCommand]s would generate if rendered.
 */
internal fun List<TerminalCommand>.lineWidths(textMetrics: TextMetrics): List<Int> {
    return this.mapLinesIntoNumericValue { text -> textMetrics.renderWidthOf(text) }
}

private fun List<TerminalCommand>.insertCommandAtLineBreaks(textMetrics: TextMetrics, commandToInsert: TerminalCommand, width: Int): List<TerminalCommand> {
    val commands = this

    return buildList {
        val lineSoFar = StringBuilder()
        for (command in commands) {
            if (command is TextCommand) {
                if (command === TextCommands.Newline) {
                    lineSoFar.clear()
                    add(command)
                } else {
                    val textSoFar = StringBuilder()
                    var currIndex = 0
                    while (currIndex <= command.text.lastIndex) {
                        val nextGraphemeSize = textMetrics.graphemeSizeAt(command.text, currIndex)
                        val nextGrapheme = command.text.subSequence(currIndex, currIndex + nextGraphemeSize)
                        currIndex += nextGraphemeSize
                        val graphemeWidth = textMetrics.renderWidthOf(nextGrapheme)
                        val remainingWidth = width - textMetrics.renderWidthOf(lineSoFar)

                        // NOTE: We insert an implicit newline when text exceeds the available space. However,
                        // we skip the newline if the render area is too narrow to fit even a single character.
                        // In that case, the character is added anyway (overflowing the width), and any
                        // necessary newline will be inserted on the subsequent iteration.
                        //
                        // For example, a 2-width character (e.g., a Chinese glyph) in a 1-width render area
                        // will simply overflow rather than being omitted entirely. This graceful degradation
                        // is preferable to dropping content, and in practice, text areas should always be
                        // significantly wider than individual characters.
                        if (lineSoFar.isNotEmpty() && graphemeWidth > remainingWidth) {
                            // NOTE: `textSoFar` will be empty if we're getting a new text command exactly at a point
                            // where we should force a newline due to crossing over the width boundary
                            if (textSoFar.isNotEmpty()) {
                                add(TextCommands.Text(textSoFar.toString()))
                            }
                            add(commandToInsert)
                            textSoFar.clear()
                            lineSoFar.clear()
                        }
                        textSoFar.append(nextGrapheme)
                        lineSoFar.append(nextGrapheme)
                    }
                    if (textSoFar.isNotEmpty()) {
                        add(TextCommands.Text(textSoFar.toString()))
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
internal fun List<TerminalCommand>.withImplicitNewlines(textMetrics: TextMetrics, width: Int): List<TerminalCommand> {
    return insertCommandAtLineBreaks(textMetrics, TextCommands.ImplicitNewline, width)
}

/**
 * Manually insert newlines into text that would go over some width.
 *
 * This can be useful for rendering text into constrained spaces, e.g. when rendering into a grid cell.
 */
internal fun List<TerminalCommand>.withExplicitNewlines(textMetrics: TextMetrics, width: Int): List<TerminalCommand> {
    return insertCommandAtLineBreaks(textMetrics, TextCommands.Newline, width)
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
