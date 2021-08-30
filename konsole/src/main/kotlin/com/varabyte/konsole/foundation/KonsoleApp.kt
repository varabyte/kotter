package com.varabyte.konsole.foundation

import com.varabyte.konsole.runtime.KonsoleApp
import com.varabyte.konsole.runtime.terminal.Terminal
import com.varabyte.konsole.terminal.SwingTerminal
import com.varabyte.konsole.terminal.SystemTerminal

fun konsoleApp(
    terminal: Terminal = run {
        try {
            SystemTerminal()
        } catch (ex: Exception) {
            SwingTerminal.create()
        }
    },
    block: KonsoleApp.() -> Unit) {
    val app = KonsoleApp(terminal).apply(block)
    app.dispose()
}