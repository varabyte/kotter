package com.varabyte.konsole.core

import com.varabyte.konsole.core.internal.MutableKonsoleTextArea
import com.varabyte.konsole.terminal.TerminalIO
import java.util.concurrent.ExecutorService

class KonsoleBlock internal constructor(
    private val executor: ExecutorService,
    private val terminalIO: TerminalIO,
    private val block: KonsoleScope.() -> Unit) {
    private val textArea = MutableKonsoleTextArea()

    internal fun applyCommand(command: KonsoleCommand) {
        command.applyTo(textArea)
    }

    fun run() {
        executor.submit {
            KonsoleScope(this).block()
            terminalIO.write(textArea.toString())
        }.get()
    }
}