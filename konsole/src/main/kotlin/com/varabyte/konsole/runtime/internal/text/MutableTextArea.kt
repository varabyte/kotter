package com.varabyte.konsole.runtime.internal.text

import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.CharCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.KonsoleTextCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.TextCommand
import com.varabyte.konsole.runtime.text.TextArea

internal class MutableTextArea : TextArea {
    private val commands = mutableListOf<KonsoleCommand>()

    override val numLines
        get() = commands.count { it === NEWLINE_COMMAND } + 1

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