package com.varabyte.konsole.core

import com.varabyte.konsole.core.internal.executor.KonsoleExecutor
import com.varabyte.konsole.terminal.SystemTerminal
import com.varabyte.konsole.terminal.Terminal
import com.varabyte.konsole.terminal.swing.SwingTerminal
import java.util.concurrent.ExecutorService

class KonsoleApp internal constructor(
    private val executor: ExecutorService = KonsoleExecutor,
    private val terminal: Terminal = run {
        try {
            SystemTerminal()
        } catch (ex: Exception) {
            SwingTerminal.create()
        }
    }
) {
    object Lifecycle : KonsoleData.Lifecycle

    private val data = KonsoleData()

    fun konsole(block: KonsoleScope.() -> Unit): KonsoleBlock = KonsoleBlock(executor, terminal, data, block)

    internal fun dispose() {
        data.dispose(Lifecycle)
    }
}

fun konsoleApp(block: KonsoleApp.() -> Unit) {
    val app = KonsoleApp().apply(block)
    app.dispose()
}