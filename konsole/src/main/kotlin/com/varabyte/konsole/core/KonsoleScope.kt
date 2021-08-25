package com.varabyte.konsole.core

class KonsoleScope(internal val block: KonsoleBlock) {
    internal var state = KonsoleBlockState()

    fun input() = block.input()

    internal fun pushState(): KonsoleBlockState {
        state = KonsoleBlockState(state)
        return state
    }

    internal fun popState() {
        check(state.parent != null) { "Called popState more times than pushState"}
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

/**
 * Run the [scopedBlock] within a fresh, new [KonsoleBlockState] context, which gets removed afterwards.
 *
 * This is useful if the scoped block is going to set one (or more) styles that are reflected in the
 * [KonsoleBlockState] class and which should only apply to that block.
 */
fun KonsoleScope.scopedState(scopedBlock: KonsoleBlock.() -> Unit) {
    pushState()
    block.scopedBlock()
    popState()
}