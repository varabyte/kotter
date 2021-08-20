package com.varabyte.konsole

import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.internal.executor.KonsoleExecutor
import com.varabyte.konsole.terminal.DefaultTerminalIO
import com.varabyte.konsole.terminal.TerminalIO
import java.util.concurrent.ExecutorService

fun konsole(
    executor: ExecutorService = KonsoleExecutor,
    terminalIO: TerminalIO = DefaultTerminalIO,
    block: KonsoleScope.() -> Unit) {
    val konsoleBlock = KonsoleBlock()

    executor.submit {
        val scope = KonsoleScope(konsoleBlock)
        scope.block()

        // Clear state for next block!
        scope.state.applyTo(konsoleBlock)

        terminalIO.write(konsoleBlock.toString())
    }.get()
}