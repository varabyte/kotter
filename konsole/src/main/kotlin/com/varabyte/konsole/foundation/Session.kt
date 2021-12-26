package com.varabyte.konsole.foundation

import com.varabyte.konsole.runtime.Session
import com.varabyte.konsole.runtime.terminal.Terminal
import com.varabyte.konsole.terminal.SystemTerminal
import com.varabyte.konsole.terminal.VirtualTerminal

/**
 * Run through a list of [Terminal] factory methods, attempting to create them in order until the first one succeeds,
 * or, if they all fail, throws an error.
 */
fun Iterable<() -> Terminal>.runUntilSuccess(): Terminal {
    return this.asSequence().mapNotNull { createTerminal ->
        try {
            createTerminal()
        }
        catch (ex: Exception) {
            System.err.println(ex)
            null
        }
    }.firstOrNull() ?: error("No terminals could successfully be created")
}

inline val DEFAULT_TERMINAL_FACTORY_METHODS get() = listOf({ SystemTerminal() }, { VirtualTerminal.create() })

fun session(
    terminal: Terminal = DEFAULT_TERMINAL_FACTORY_METHODS.runUntilSuccess(),
    block: Session.() -> Unit) {

    val app = Session(terminal)
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