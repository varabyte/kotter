package com.varabyte.kotter.runtime.render

import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.kotter.runtime.internal.ansi.commands.RESET_COMMAND
import com.varabyte.kotter.runtime.internal.text.MutableTextArea
import com.varabyte.kotter.runtime.text.TextArea

/**
 * A class responsible for executing some block of logic which contains render instructions, which ultimately modify an
 * internal [MutableTextArea].
 */
class Renderer(val app: Session, val autoAppendNewline: Boolean = true) {
    private val _textArea = MutableTextArea()
    internal val commands: List<TerminalCommand> = _textArea.commands
    val textArea: TextArea = _textArea

    /** Append this command to the end of this block's text area */
    internal fun appendCommand(command: TerminalCommand) {
        _textArea.appendCommand(command)
    }

    internal fun render(block: RenderScope.() -> Unit) {
        _textArea.clear()
        RenderScope(this).apply {
            block()
            // Make sure we clear all state as we exit this block. This ensures that repaint passes don't carry
            // state leftover from its end back to the beginning.
            _textArea.appendCommand(RESET_COMMAND)
        }

        if (autoAppendNewline && textArea.toRawText().lastOrNull() != '\n') {
            _textArea.appendCommand(NEWLINE_COMMAND)
        }
    }
}