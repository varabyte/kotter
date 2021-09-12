package com.varabyte.konsole.foundation

import com.varabyte.konsole.runtime.KonsoleApp
import com.varabyte.konsole.runtime.terminal.Terminal
import com.varabyte.konsole.terminal.SystemTerminal
import com.varabyte.konsole.terminal.VirtualTerminal

fun konsoleApp(
    terminal: Terminal = SystemTerminal.or { VirtualTerminal.create() },
    block: KonsoleApp.() -> Unit) {
    val app = KonsoleApp(terminal)
    Runtime.getRuntime().addShutdownHook(Thread {
        // Clean-up even if the user presses control-C
        app.dispose()
    })

    try {
        app.apply(block)
        app.assertNoActiveBlocks()
    }
    finally {
        app.dispose()
    }
}