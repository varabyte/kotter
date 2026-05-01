package com.varabyte.kotter.terminal.virtual

import java.awt.Graphics2D
import java.awt.Rectangle
import javax.swing.JComponent

/**
 * A service that external projects can implement to take over rendering emoji characters in the virtual terminal.
 */
interface EmojiRenderer {
    /**
     * Render the specified grapheme into the requested component location, returning false if not handled.
     *
     * @param g2d The graphics context that handles Swing rendering.
     * @param component The component that is rendered into.
     * @param grapheme The emoji grapheme itself. This will never be anything but an emoji sequence.
     * @param bounds The bounds of where the emoji should be rendered within.
     */
    fun render(g2d: Graphics2D, component: JComponent, grapheme: String, bounds: Rectangle): Boolean
}
