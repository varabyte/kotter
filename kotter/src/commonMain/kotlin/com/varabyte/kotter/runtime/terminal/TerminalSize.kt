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
        /** Convenience constructor for creating a terminal size with a fixed width and unbounded height. */
        fun ofWidth(value: Int) = TerminalSize(value, Unbounded.height)
    }
    init {
        require(width >= 1 && height >= 1) { "TerminalSize values must both be positive. Got: $width, $height" }
    }

    fun copy(width: Int = this.width, height: Int = this.height): TerminalSize =
        if (this.width != width || this.height != height) TerminalSize(width, height) else this

    override fun equals(other: Any?): Boolean {
        return other is TerminalSize && other.width == width && other.height == height
    }

    override fun hashCode(): Int {
        return arrayOf(width, height).contentHashCode()
    }

    override fun toString() = "TerminalSize($width, $height)"
}

/**
 * Given [width] and [height] values, either create a new TerminalSize or re-use the same underlying object.
 *
 * This convenience method allows us to shortcut this pattern boilerplate which was showing up a couple of times in our
 * codebase:
 * ```
 * private val _terminalSize: TerminalSize? = null
 * val terminalSize: TerminalSize get() {
 *    val w = queryWidth()
 *    val h = queryHeight
 *    if (_terminalSize == null || _terminalSize.width != w || _terminalSize.height != h) {
 *        _terminalSize = TerminalSize(width, height)
 *    }
 *    return _terminalSize!!
 * }
 * ```
 *
 * which can now just be:
 * ```
 * private val _terminalSize: TerminalSize? = null
 * val terminalSize: TerminalSize {
 *    return _terminalSize.createOrReuse(queryWidth(), queryHeight()).also { _terminalSize = it }
 * }
 * ```
 */
fun TerminalSize?.createOrReuse(width: Int, height: Int): TerminalSize {
    // Note: `copy` returns `this` instead of a new value if width / height values are the same
    return this?.copy(width, height) ?: TerminalSize(width, height)
}
