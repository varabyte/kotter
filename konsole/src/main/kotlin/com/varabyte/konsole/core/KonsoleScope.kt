package com.varabyte.konsole.core

class KonsoleScope(internal val block: KonsoleBlock) {
    internal var state = KonsoleState()

    internal fun pushState(): KonsoleState {
        state = KonsoleState(state)
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
 * Run the [scopedBlock] within a fresh, new [KonsoleState] context, which gets removed afterwards.
 *
 * This is useful if the scoped block is going to set one (or more) styles that are reflected in the
 * [KonsoleState] class and which should only apply to that block.
 */
fun KonsoleScope.scopedState(scopedBlock: KonsoleBlock.() -> Unit) {
    pushState()
    block.scopedBlock()
    popState()
}
