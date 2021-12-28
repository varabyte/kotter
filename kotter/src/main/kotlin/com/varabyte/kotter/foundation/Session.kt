package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.terminal.Terminal
import com.varabyte.kotter.terminal.SystemTerminal
import com.varabyte.kotter.terminal.VirtualTerminal

/**
 * Run through a list of [Terminal] factory methods, attempting to create them in order until the first one succeeds,
 * or, if they all fail, throws an error.
 */
fun Iterable<() -> Terminal>.runUntilSuccess(): Terminal {
    val creationErrors = mutableListOf<Exception>()
    return this.asSequence().mapNotNull { createTerminal ->
        try {
            createTerminal()
        }
        catch (ex: Exception) {
            creationErrors.add(ex)
            null
        }
    }.firstOrNull() ?: error("No terminals could successfully be created. Encountered exceptions:\n\t{${creationErrors.joinToString("\n\t")}")
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