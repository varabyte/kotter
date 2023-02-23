package com.varabyte.kotter.runtime.terminal

import kotlinx.coroutines.flow.Flow

/**
 * An interface for abstracting input and output for various terminal implementations.
 */
interface Terminal : AutoCloseable {
    /**
     * The width of the terminal.
     *
     * If set, text written to the terminal will have newlines auto-appended.
     */
    val width: Int get() = Int.MAX_VALUE

    /**
     * Write some text to the underlying terminal.
     *
     * This text may have ANSI control characters in it.
     */
    fun write(text: String)

    /**
     * Return a hot [Flow] which will get triggered with characters read in by the underlying terminal, often input
     * typed in by a user.
     *
     * Note that these characters may represent encodings for actions, for example LEFT will be the character sequence
     * `ESC, [, D`.
     */
    fun read(): Flow<Int>

    /**
     * Clear the current terminal, removing all text written there so far.
     */
    fun clear()
}