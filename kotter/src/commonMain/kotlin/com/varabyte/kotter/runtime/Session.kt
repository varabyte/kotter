package com.varabyte.kotter.runtime

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.shutdown.*
import com.varabyte.kotter.runtime.concurrent.*
import com.varabyte.kotter.runtime.coroutines.*
import com.varabyte.kotter.runtime.terminal.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * A Kotter session.
 *
 * This class represents the lifetime of a Kotter application.
 *
 * When a session exits, all data associated with it will be released.
 *
 * You cannot create an instance manually. Instead, use [session].
 */
class Session internal constructor(
    internal val terminal: Terminal,
    internal val sectionExceptionHandler: (Throwable) -> Unit
) {
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

    init {
        @Suppress("RemoveRedundantQualifierName") // Useful to show "Session.Lifecycle" for readability
        data.start(Session.Lifecycle)
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

    /**
     * Called in response to the user forcefully exiting the program, i.e. via ctrl+C.
     */
    internal fun shutdown() {
        data.get(SessionShutdownHookKey) { this.forEach { it.invoke() } }
        data.get(SectionShutdownHookKey) { this.forEach { it.invoke() } }
        if (activeSection != null) {
            // If there's a section currently running, it's *possible* that one of the shutdown
            // hooks modified a livevar, so let's give the renderer one more frame to run just in case.
            val renderingFinished = CompletableDeferred<Unit>()
            CoroutineScope(KotterDispatchers.Render).launch { renderingFinished.complete(Unit) }
            runBlocking { renderingFinished.await() }
        }

        dispose()
    }

    internal fun dispose() {
        @Suppress("RemoveRedundantQualifierName") // Useful to show "Session.Lifecycle" for readability
        if (data.isActive(Session.Lifecycle)) {
            data.stopAll()
            terminal.close()
        }
    }
}
