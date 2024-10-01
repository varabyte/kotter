package com.varabyte.kotter.runtime.terminal

/**
 * The size of the virtual terminal text area, where [width] and [height] represent the number of characters that can
 * fit within it.
 *
 * In other words, 80x32 means 80 characters wide by 32 lines tall, as opposed to 80 pixels by 32 pixels.
 */
class TerminalSize(val width: Int, val height: Int) {
    companion object {
        val Default = TerminalSize(100, 40)
        val Unbounded = TerminalSize(Int.MAX_VALUE, Int.MAX_VALUE)
    }
    init {
        require(width >= 1 && height >= 1) { "TerminalSize values must both be positive. Got: $width, $height" }
    }
}
