package com.varabyte.kotter.runtime.terminal

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * An interface for abstracting input and output for various terminal implementations.
 */
interface Terminal {
    interface Events {
        /**
         * A flow that emits a new [TerminalSize] whenever the terminal is resized.
         *
         * After this event is triggered, [width] and [height] properties can be re-queried for new values.
         */
        val sizeChanged: SharedFlow<TerminalSize>
    }

    /**
     * A class that allows triggering the events from [Events].
     *
     * To use it, declare the following in your own terminal class:
     *
     * ```
     * private val mutableEvents = Terminal.MutableEvents()
     * override val events: Terminal.Events = mutableEvents.toEvents()
     * // later...
     * mutableEvents.xyz(arg)
     * // at which point, `events.xyz.collect { ... }` listeners will be triggered
     * ```
     */
    class MutableEvents {
        companion object {
            /** Create a mutable flow with settings useful to be used for events */
            private fun <T> createMutableFlow(): MutableSharedFlow<T> {
                return MutableSharedFlow(
                    replay = 0, // Only coroutines already collecting the event should be notified
                    // A small buffer allows a bit of grace for cross-thread coroutines to communicate, apparently
                    extraBufferCapacity = 1,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST,
                )
            }
        }

        val sizeChanged = createMutableFlow<TerminalSize>()

        fun asReadOnly() = object : Events {
            override val sizeChanged: SharedFlow<TerminalSize> = this@MutableEvents.sizeChanged.asSharedFlow()
        }
    }

    val events: Events

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
     * Return a hot [SharedFlow] which will get triggered with characters read in by the underlying terminal, often
     * input typed in by a user.
     *
     * Note that these characters may represent encodings for actions, for example LEFT will be the character sequence
     * `ESC, [, D`.
     */
    fun read(): SharedFlow<Int>

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