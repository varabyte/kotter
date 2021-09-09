package com.varabyte.konsole.foundation

import com.varabyte.konsole.runtime.KonsoleApp
import com.varabyte.konsole.runtime.terminal.Terminal
import com.varabyte.konsole.terminal.SystemTerminal
import com.varabyte.konsole.terminal.VirtualTerminal

fun konsoleApp(
    terminal: Terminal = SystemTerminal.or { VirtualTerminal.create() },
    block: KonsoleApp.() -> Unit) {
    val app = KonsoleApp(terminal)
    try {
        app.apply(block)
    }
    finally {
        app.dispose()
    }
}