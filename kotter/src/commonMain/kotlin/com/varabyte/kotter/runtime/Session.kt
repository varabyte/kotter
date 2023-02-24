package com.varabyte.kotter.runtime

import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData
import com.varabyte.kotter.runtime.terminal.Terminal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext

/**
 * A Kotter session.
 *
 * This class represents the lifetime of a Kotter application.
 *
 * When a session exits, all data associated with it will be released.
 *
 * You cannot create an instance manually. Instead, use [session].
 */
class Session internal constructor(internal val terminal: Terminal) {
    class Dispatchers {
        private val _render = newSingleThreadContext("Kotter Render")
        val render: CoroutineDispatcher = _render
        private val _io = newFixedThreadPoolContext(50, "Kotter I/O")
        val io: CoroutineDispatcher = _io

        internal fun close() {
            _render.close()
            _io.close()
        }
    }

    /**
     * A long-lived lifecycle that sticks around for the length of the entire session.
     *
     * This lifecycle can be used for storing data that should live across multiple blocks.
     */
    object Lifecycle : ConcurrentScopedData.Lifecycle

    val data = ConcurrentScopedData()

    /**
     * A way to access the current active section, if any.
     *
     * You normally shouldn't need to use this, but it can be good for tests, or as a way to abort a running Kotter
     * program from a different thread that isn't blocked by a call to [Section.run].
     */
    val activeSection: Section? get() = data[ActiveSectionKey]

    val dispatchers = Dispatchers()

    init {
        data.start(Lifecycle)
    }

    internal fun assertNoActiveSections() {
        check(!data.isActive(Section.Lifecycle)) {
            "A previous section was created but unused. Did you forget to call `run` on it?"
        }
    }

    /**
     * Create a Kotter section.
     *
     * A `section` block owns a bunch of rendering instructions, followed by a `run` block which executes them.
     *
     * For example:
     *
     * ```
     * session {
     *   section { ... render instructions ...}.run()
     * }
     * ```
     */
    fun section(render: MainRenderScope.() -> Unit): Section {
        assertNoActiveSections()
        return Section(this, render)
    }

    internal fun dispose() {
        // Protect against dispose being called multiple times
        if (data.isActive(Lifecycle)) {
            data.stopAll()
            terminal.close()

            dispatchers.close()
        }
    }
}