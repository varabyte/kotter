package com.varabyte.kotter.runtime.terminal

import kotlinx.coroutines.flow.Flow

/**
 * An interface for abstracting input and output for various terminal implementations.
 */
interface Terminal {
    /**
     * The width of the terminal.
     *
     * Once this width is reached, newlines will be auto-appended. It will also be used in calculating how many
     * lines to erase on repaint.
     */
    val width: Int

    /**
     * The height of the terminal.
     *
     * This is used to ensure we don't try to render more lines than what fit on the screen.
     */
    val height: Int

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

    /**
     * Shut down this terminal, releasing any resources.
     *
     * It is an error to use this terminal instance after it has been closed.
     */
    fun close()
}