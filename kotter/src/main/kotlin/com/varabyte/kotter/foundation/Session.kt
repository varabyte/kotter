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

/**
 * @param clearTerminal Set to true if this program should clear the terminal on startup. Defaulted to false since that
 *   might be surprising behavior for simple utility terminal applications.
 */
fun session(
    terminal: Terminal = listOf({ SystemTerminal() }, { VirtualTerminal.create() }).runUntilSuccess(),
    clearTerminal: Boolean = false,
    block: Session.() -> Unit
) {

    if (clearTerminal) terminal.clear()

    val session = Session(terminal)
    Runtime.getRuntime().addShutdownHook(Thread {
        // Clean-up even if the user presses control-C
        session.dispose()
    })

    try {
        session.apply(block)
        session.assertNoActiveSections()
    }
    finally {
        session.dispose()
    }
}