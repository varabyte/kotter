package com.varabyte.konsole.core

class KonsoleScope(private val block: KonsoleBlock) {

    internal var state = KonsoleState()

    /** Data which is tied to the current app. */
    val data get() = block.app.data

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

    /** Create a [KonsoleVar] whose scope is tied to this app. */
    @Suppress("FunctionName") // Intentionally made to look like a class constructor
    fun <T> KonsoleVar(value: T): KonsoleVar<T> = block.app.KonsoleVar(value)
    /** Create a [KonsoleList] whose scope is tied to this app. */
    @Suppress("FunctionName") // Intentionally made to look like a class constructor
    fun <T> KonsoleList(vararg elements: T): KonsoleList<T> = block.app.KonsoleList(*elements)

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