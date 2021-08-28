package com.varabyte.konsole.runtime

import com.varabyte.konsole.runtime.concurrent.ConcurrentData
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
 * the concept of the block itself is represented by the [KonsoleBlock] class, while each render pass (triggered every
 * 250ms, above) is passed a new instance of this class.
 *
 * Despite this class's transient state, it often works with the underlying block, offering a limited subset of passthru
 * functionality that simply delegates the operation to the underlying block.
 */
class RenderScope(private val block: KonsoleBlock) {
    /**
     * A very short lifecycle that lives for a single block render pass.
     *
     * This could be useful for verifying that a method with a side effect was only executed once, for example.
     */
    object Lifecycle : ConcurrentData.Lifecycle

    /** Data which is tied to the current app. */
    val data get() = block.app.data

    /** The current app's terminal */
    internal val terminal get() = block.app.terminal

    internal var state = KonsoleState()
    internal val lastChar: Char? get() = block.lastChar

    /**
     * Run the [scopedBlock] within a fresh, new [KonsoleState] context, which gets removed afterwards.
     *
     * This is useful if the scoped block is going to set one (or more) styles that are reflected in the
     * [KonsoleState] class and which should only apply to that block.
     */
    fun scopedState(scopedBlock: KonsoleBlock.() -> Unit) {
        pushState()
        block.scopedBlock()
        popState()
    }

    internal fun requestRerender() = block.requestRerender()

    private fun pushState(): KonsoleState {
        state = KonsoleState(state)
        return state
    }

    private fun popState() {
        check(state.parent != null) { "Called popState more times than pushState" }
        state.parent!!.let { prevState ->
            if (state.isDirty) {
                prevState.applyTo(block, force = true)
            }
            state = prevState
        }
    }

    internal fun applyCommand(command: KonsoleCommand) {
        command.updateState(state)
        block.applyCommand(command)
    }
}