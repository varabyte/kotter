package com.varabyte.konsole.runtime.internal

import com.varabyte.konsole.runtime.KonsoleState
import com.varabyte.konsole.runtime.render.Renderer

internal open class KonsoleCommand(val text: String) {
    /**
     * Apply this command either to the current state (if it updates the active effects) or the text block.
     */
    open fun applyTo(state: KonsoleState, renderer: Renderer) {
        renderer.appendCommand(this)
    }
}