package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.terminal.Terminal
import com.varabyte.kotter.terminal.system.SystemTerminal
import com.varabyte.kotter.terminal.virtual.VirtualTerminal

/**
 * Run through a list of [Terminal] factory methods, attempting to create them in order until the first one succeeds,
 * or, if they all fail, throws an error.
 *
 * This is a useful utility method for those who want to construct a [Session] with their own custom list of
 * terminals. For example:
 *
 * ```
 * session(
 *   terminal = listOf(
 *     { FirstTerminal() },
 *     { SecondTerminal() },
 *   ).firstSuccess()
 * )
 * ```
 */
fun Iterable<() -> Terminal>.firstSuccess(): Terminal {
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
 * Create a Kotter session.
 *
 * This method takes in a block which will be scoped to the lifetime of the current session. It is a place you can
 * create `section`s and also declare `liveVarOf` calls:
 *
 * ```
 * session {
 *   var counter by liveVarOf(0)
 *   section { ... }
 * }
 * ```
 *
 * When the session exists, all data associated with it will be released. If Kotter runs within a virtual terminal, then
 * the lifetime of the virtual terminal is that of the session.
 *
 * @param clearTerminal Set to true if this program should clear the terminal on startup. Defaulted to false since that
 *   might be surprising behavior for simple utility terminal applications.
 */
fun session(
    terminal: Terminal = listOf({ SystemTerminal() }, { VirtualTerminal.create() }).firstSuccess(),
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