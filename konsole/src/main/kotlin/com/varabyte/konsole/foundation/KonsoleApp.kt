package com.varabyte.konsole.foundation

import com.varabyte.konsole.runtime.KonsoleApp
import com.varabyte.konsole.runtime.internal.executor.KonsoleExecutor
import com.varabyte.konsole.runtime.terminal.Terminal
import com.varabyte.konsole.terminal.SwingTerminal
import com.varabyte.konsole.terminal.SystemTerminal
import java.util.concurrent.ExecutorService

fun konsoleApp(
    executor: ExecutorService = KonsoleExecutor,
    terminal: Terminal = run {
        try {
            SystemTerminal()
        } catch (ex: Exception) {
            SwingTerminal.create()
        }
    },
    block: KonsoleApp.() -> Unit) {
    val app = KonsoleApp(executor, terminal).apply(block)
    app.dispose()
}