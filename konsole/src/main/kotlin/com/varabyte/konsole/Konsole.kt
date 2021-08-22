package com.varabyte.konsole

import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.internal.executor.KonsoleExecutor
import com.varabyte.konsole.terminal.DefaultTerminalIO
import com.varabyte.konsole.terminal.Terminal
import java.util.concurrent.ExecutorService

fun konsole(
    executor: ExecutorService = KonsoleExecutor,
    terminal: Terminal = DefaultTerminalIO,
    block: KonsoleScope.() -> Unit): KonsoleBlock {

    return KonsoleBlock(executor, terminal, block)
}