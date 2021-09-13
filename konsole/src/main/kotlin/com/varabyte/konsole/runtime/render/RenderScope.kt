package com.varabyte.konsole.runtime.render

import com.varabyte.konsole.runtime.KonsoleState
import com.varabyte.konsole.runtime.concurrent.ConcurrentScopedData
import com.varabyte.konsole.runtime.internal.KonsoleCommand

/**
 * A scope which represents a single render pass inside a Konsole block.
 *
 * So for this simple, never-ending example:
 *
 * ```
 * var count by KonsoleVar(0)
 * konsole {
 *   textLine("Count: $count")
 * }.run {
 *   while (true) {
 *     delay(250)
 *     ++count
 *   }
 * }
 * ```
 *
 * the part between the `konsole` curly braces represents a render block, where `this` is a `RenderScope`.
 */
class RenderScope(internal val renderer: Renderer) {
    internal var state = KonsoleState()

    /**
     * Data store for this app.
     *
     * It is exposed directly and publicly here so methods extending the RunScope can use it.
     */
    val data = renderer.app.data

    /**
     * Run the [scopedBlock] within a fresh, new [KonsoleState] context, which gets removed afterwards.
     *
     * This is useful if the scoped block is going to set one (or more) styles that are reflected in the
     * [KonsoleState] class and which should only apply to that block.
     */
    fun scopedState(scopedBlock: RenderScope.() -> Unit) {
        pushState()
        scopedBlock()
        popState()
    }

    private fun pushState(): KonsoleState {
        state = KonsoleState(state)
        return state
    }

    private fun popState() {
        check(state.parent != null) { "Called popState more times than pushState" }
        state = state.parent!!
    }

    internal fun applyCommand(command: KonsoleCommand) {
        command.applyTo(state, renderer)
    }
}