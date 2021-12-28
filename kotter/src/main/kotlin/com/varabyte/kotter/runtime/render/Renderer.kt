package com.varabyte.kotter.runtime.render

import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.kotter.runtime.internal.ansi.commands.RESET_COMMAND

/**
 * A class responsible for executing some block of logic which requests render instructions, which ultimately modify an
 * internal [TerminalCommand] list.
 */
class Renderer(val session: Session) {
    private val _commands = mutableListOf<TerminalCommand>()
    internal val commands: List<TerminalCommand> = _commands

    /** Append this command to the end of this block's text area */
    internal fun appendCommand(command: TerminalCommand) {
        _commands.add(command)
    }

    internal fun render(block: RenderScope.() -> Unit) {
        _commands.clear()
        RenderScope(this).apply {
            block()

            if (_commands.lastOrNull() !== NEWLINE_COMMAND) {
                _commands.add(NEWLINE_COMMAND)
            }

            // Make sure we clear all state as we exit this block. This ensures that repaint passes don't carry
            // state leftover from its end back to the beginning.
            _commands.add(RESET_COMMAND)
        }
    }
}