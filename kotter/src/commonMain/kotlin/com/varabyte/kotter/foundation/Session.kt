package com.varabyte.kotter.foundation

import com.varabyte.kotter.platform.internal.system.onShutdown
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.terminal.Terminal

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

internal expect val defaultTerminalProviders: List<() -> Terminal>

/**
 * Create a Kotter session.
 *
 * This method takes in a block which will be scoped to the lifetime of the created session. It is a place you can
 * create `section`s and also declare `liveVarOf` (and similar) calls:
 *
 * ```
 * session {
 *   var counter by liveVarOf(0)
 *   section { ... }
 * }
 * ```
 *
 * When the session exits, all data associated with it will be released. If Kotter runs within a virtual terminal, then
 * the lifetime of the window is essentially the same as that of the session.
 *
 * @param terminal The terminal implementation backing this session.
 * @param clearTerminal Set to true if this program should clear the terminal on startup. Defaulted to false since that
 *   might be surprising behavior for simple utility terminal applications.
 */
fun session(
    terminal: Terminal = defaultTerminalProviders.firstSuccess(),
    clearTerminal: Boolean = false,
    block: Session.() -> Unit
) {

    if (clearTerminal) terminal.clear()

    val session = Session(terminal)

    // Clean-up even if the user presses control-C
    onShutdown { session.dispose() }

    try {
        session.apply(block)
        session.assertNoActiveSections()
    }
    finally {
        session.dispose()
    }
}