package com.varabyte.kotter.runtime

import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotter.runtime.terminal.Terminal
import java.util.concurrent.Executors

class Session internal constructor(internal val terminal: Terminal) {
    /**
     * A long-lived lifecycle that sticks around for the length of the entire session.
     *
     * This lifecycle can be used for storing data that should live across multiple blocks.
     */
    object Lifecycle : ConcurrentScopedData.Lifecycle

    val data = ConcurrentScopedData()

    internal val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "Kotter Thread").apply {
            // This thread is never stopped so allow the program to quit even if it's running
            isDaemon = true
        }
    }
    internal val activeSection: Section? get() = data[ActiveSectionKey]

    init {
        data.start(Lifecycle)
    }

    internal fun assertNoActiveSections() {
        check(!data.isActive(Section.Lifecycle)) {
            "A previous section was created but unused. Did you forget to call `run` on it?"
        }
    }

    fun section(render: MainRenderScope.() -> Unit): Section {
        assertNoActiveSections()
        return Section(this, render)
    }

    internal fun dispose() {
        // Protect against dispose being called multiple times
        if (data.isActive(Lifecycle)) {
            data.stopAll()
            terminal.close()
        }
    }
}