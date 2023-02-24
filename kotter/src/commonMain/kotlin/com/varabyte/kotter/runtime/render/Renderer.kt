package com.varabyte.kotter.runtime.render

import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.kotter.runtime.internal.ansi.commands.RESET_COMMAND
import com.varabyte.kotter.runtime.internal.ansi.commands.TextCommand

/**
 * A class responsible for executing some block of logic which requests render instructions, ultimately converting them
 * into a list of commands that can be handled by Kotter.
 *
 * @property session The parent session this renderer is tied to. This class makes no use of it, but some places that
 *   work with a renderer do.
 */
class Renderer<R: RenderScope>(val session: Session, private val createScope: (Renderer<R>) -> R) {
    private val _commands = mutableListOf<TerminalCommand>()
    internal val commands: List<TerminalCommand> = _commands

    /** Append this command to the end of this block's text area */
    internal fun appendCommand(command: TerminalCommand) {
        _commands.add(command)
    }

    internal fun render(render: R.() -> Unit) {
        _commands.clear()

        createScope(this).render()

        _commands.add(RESET_COMMAND)

        if (commands.asSequence().filter { it is TextCommand }.lastOrNull() !== NEWLINE_COMMAND) {
            _commands.add(NEWLINE_COMMAND)
        }
    }
}