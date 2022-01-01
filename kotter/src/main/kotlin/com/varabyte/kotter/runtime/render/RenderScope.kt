package com.varabyte.kotter.runtime.render

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
class RenderScope(internal val renderer: Renderer) {
    internal var state = SectionState()

    /**
     * Data store for this session.
     *
     * It is exposed directly and publicly here so methods extending the RunScope can use it.
     */
    val data = renderer.session.data

    /**
     * Run the [scopedBlock] within a fresh, new [SectionState] context, which gets removed afterwards.
     *
     * This is useful if the scoped block is going to set one (or more) styles that are reflected in the
     * [SectionState] class and which should only apply to that block.
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