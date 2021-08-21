package com.varabyte.konsole.core.block

class BackgroundWorkScope(
    private val rerenderRequested: () -> Unit
) {
    var rerenderOnFinished = true
    fun rerender() = rerenderRequested()
}
