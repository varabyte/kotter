package com.varabyte.konsole.core

class KonsoleScope(private val block: KonsoleBlock) {

    internal var state = KonsoleState()

    /**
     * Data which is tied to the underlying block (and may live across multiple intermediate scopes)
     */
    val data get() = block.data

    /**
     * A flow of keypresses that you can collect.
     *
     * It's recommended to collect these keys on the IO dispatcher.
     */
    val keyFlow get() = block.keyFlow

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

    internal fun pushState(): KonsoleState {
        state = KonsoleState(state)
        return state
    }

    internal fun popState() {
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