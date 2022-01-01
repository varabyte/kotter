package com.varabyte.kotter.runtime.internal

import com.varabyte.kotter.runtime.SectionState
import com.varabyte.kotter.runtime.render.Renderer

internal open class TerminalCommand(val text: String) {
    /**
     * Apply this command either to the current state (if it updates the active effects) or the text block.
     */
    open fun applyTo(state: SectionState, renderer: Renderer<*>) {
        renderer.appendCommand(this)
    }
}