package com.varabyte.konsole.foundation

import com.varabyte.konsole.runtime.KonsoleApp
import com.varabyte.konsole.runtime.terminal.Terminal
import com.varabyte.konsole.terminal.SystemTerminal
import com.varabyte.konsole.terminal.VirtualTerminal

fun konsoleApp(
    terminal: Terminal = run {
        try {
            SystemTerminal()
        } catch (ex: Exception) {
            VirtualTerminal.create()
        }
    },
    block: KonsoleApp.() -> Unit) {
    val app = KonsoleApp(terminal).apply(block)
    app.dispose()
}