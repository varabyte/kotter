package com.varabyte.kotter.runtime.internal.text

import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.TextCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.kotter.runtime.text.TextArea

internal class MutableTextArea : TextArea {
    internal val commands = mutableListOf<TerminalCommand>()

    override val lineLengths: List<Int>
        get() {
            return commands.fold(mutableListOf(0)) { lengths, command ->
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

    override fun isEmpty() = commands.isEmpty()

    fun clear(): MutableTextArea {
        commands.clear()
        return this
    }

    fun appendCommand(command: TerminalCommand) { commands.add(command) }

    override fun toRawText(): String {
        return commands
            .filterIsInstance<TextCommand>()
            .joinToString("") { it.text }
    }

    override fun toString(): String {
        return commands.joinToString("") { it.text }
    }
}