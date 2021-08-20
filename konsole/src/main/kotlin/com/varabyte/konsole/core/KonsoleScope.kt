package com.varabyte.konsole.core

class KonsoleScope(internal val block: KonsoleBlock) {
    internal var state = KonsoleState()

    fun pushState(): KonsoleState {
        state = KonsoleState(state)
        return state
    }

    fun popState() {
        check(state.parent != null) { "Called popState more times than pushState"}
        state = state.parent!!
    }
}

/**
 * Run the [scopedBlock] within a fresh, new [KonsoleState] context, which gets removed afterwards.
 *
 * This is useful if the scoped block is going to set one (or more) styles that are reflected in the
 * [KonsoleState] class and which should only apply to that block.
 */
internal fun KonsoleScope.createScopedState(scopedBlock: KonsoleBlock.() -> Unit) {
    pushState()
    block.scopedBlock()
    popState()
}
