package com.varabyte.kotter.runtime.render

import com.varabyte.kotter.runtime.SectionScope
import com.varabyte.kotter.runtime.SectionState
import com.varabyte.kotter.runtime.internal.TerminalCommand

/**
 * A scope which represents a single render pass inside a section.
 *
 * So for this simple, never-ending example:
 *
 * ```
 * var count by liveVarOf(0)
 * section {
 *   textLine("Count: $count")
 * }.run {
 *   while (true) {
 *     delay(250)
 *     ++count
 *   }
 * }
 * ```
 *
 * the part between the `section` curly braces represents a render block, where `this` is a `RenderScope`.
 */
abstract class RenderScope(internal val renderer: Renderer<*>): SectionScope {
    internal var state = SectionState()

    /**
     * Data store for this session.
     *
     * It is exposed directly and publicly here so methods extending this [RenderScope] can use it.
     */
    override val data = renderer.session.data

    /**
     * The current section this block is being rendered into.
     *
     * It is an error to call this outside of a running section, which shouldn't happen in practice unless you go out of
     * your way to keep a reference to a stale render scope after a section has ended.
     */
    val section get() = renderer.session.activeSection!!

    /**
     * Run the [scopedBlock] within a new context within which any state changes will be cleared after it exits.
     */
    fun scopedState(scopedBlock: RenderScope.() -> Unit) {
        pushState()
        scopedBlock()
        popState()
    }

    private fun pushState(): SectionState {
        state = SectionState(state)
        return state
    }

    private fun popState() {
        check(state.parent != null) { "Called popState more times than pushState" }
        state = state.parent!!
    }

    internal fun applyCommand(command: TerminalCommand) {
        command.applyTo(state, renderer)
    }
}

/** Marker base class for render scopes that are only ever meant to fire once */
abstract class OneShotRenderScope(renderer: Renderer<*>): RenderScope(renderer)