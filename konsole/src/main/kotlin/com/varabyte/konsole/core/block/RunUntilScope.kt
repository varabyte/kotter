package com.varabyte.konsole.core.block

class RunUntilScope(
    private val rerenderRequested: () -> Unit
) {
    fun rerender() = rerenderRequested()
}
