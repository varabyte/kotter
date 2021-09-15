package com.varabyte.konsole.runtime.internal.text

import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.CharCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.KonsoleTextCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.TextCommand
import com.varabyte.konsole.runtime.text.TextArea

internal class MutableTextArea : TextArea {
    private val commands = mutableListOf<KonsoleCommand>()

    override fun numLines(width: Int): Int {
        val lineLengths = commands.fold(mutableListOf(0)) { acc, command ->
            if (command is KonsoleTextCommand) {
                if (command === NEWLINE_COMMAND) {
                    acc.add(0)
                }
                else {
                    acc[acc.lastIndex] += command.text.length
                }
            }
            acc
        }
        return lineLengths.size +
                // The line gets an implicit newline once it goes ONE over the terminal width - or in other
                // words, a 20 character line fits perfectly in a 20 column terminal, so don't treat that case
                // as an extra newline until we hit 21 characters
                lineLengths.sumOf { len -> (len - 1) / width }
    }

    override fun isEmpty() = commands.isEmpty()

    fun clear(): MutableTextArea {
        commands.clear()
        return this
    }

    fun appendCommand(command: KonsoleCommand) { commands.add(command) }

    override fun toRawText(): String {
        return commands
            .filterIsInstance<KonsoleTextCommand>()
            .joinToString("") { it.text }
    }

    override fun toString(): String {
        return commands.joinToString("") { it.text }
    }
}