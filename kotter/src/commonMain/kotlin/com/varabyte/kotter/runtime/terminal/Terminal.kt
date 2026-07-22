package com.varabyte.kotter.runtime.terminal

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
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
         * After this event is triggered, [size] property can be re-queried for new values.
         */
        val sizeChanged: Flow<TerminalSize>
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
            override val sizeChanged: Flow<TerminalSize> = this@MutableEvents.sizeChanged.asSharedFlow()
        }
    }

    val events: Events

    /**
     * The size of the terminal.
     *
     * If any text runs past this size's width, newlines will be auto-appended. The width is also used to calculate how
     * many lines to erase on repaint.
     *
     * If any lines are added past this size's height, earlier lines are dropped.
     */
    val size: TerminalSize

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