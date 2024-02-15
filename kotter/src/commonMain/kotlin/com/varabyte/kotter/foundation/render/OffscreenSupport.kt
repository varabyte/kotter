package com.varabyte.kotter.foundation.render

import com.varabyte.kotter.runtime.*
import com.varabyte.kotter.runtime.internal.*
import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.internal.text.*
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotterx.decorations.*
import com.varabyte.kotterx.text.*

/**
 * An internal buffer that stores commands without actively rendering them.
 *
 * As a user, you won't create one yourself, but you'll use [offscreen] to do so.
 *
 * This class itself is inert, but you can use [createRenderer] to create another class that can apply its commands to
 * the current render scope.
 *
 * @property parentScope The [RenderScope] this buffer is tied to. This parameter is exposed for testing.
 */
class OffscreenBuffer internal constructor(
    internal val parentScope: RenderScope,
    maxWidth: Int,
    render: OffscreenRenderScope.() -> Unit
) {
    private val commands = run {
        val offscreenRenderer =
            Renderer(parentScope.renderer.session) { OffscreenRenderScope(it) }.apply {
                render(render)
            }
        // The renderer normally makes sure that a command block ends with a trailing newline and a state reset, but we
        // don't need those in an offscreen buffer.
        // 1) The final newline shouldn't leak into the output. For example,
        // `offscreen { lines.forEach { textLine(it } }` shouldn't create a trailing single empty line.
        // 2) `CommandRenderer` below handles its own state cleanup logic
        // Note: The order of the newline and reset depend on if the final row ended with a newline or not. Either way,
        // we want to remove both of them!
        check(offscreenRenderer.commands.takeLast(2).containsAll(listOf(NEWLINE_COMMAND, RESET_COMMAND)))
        offscreenRenderer.commands.dropLast(2)
    }.withExplicitNewlines(maxWidth)

    /**
     * A property which provides access to the lengths of each line in the buffer.
     *
     * This can be used if you need to calculate padding, for example, or centering a text block.
     */
    val lineLengths = commands.lineLengths

    /**
     * Create an [OffscreenCommandRenderer] which can apply this buffer's commands into the current render scope.
     *
     * While most cases will only ever require a single renderer, you can create multiple renderers at the same time,
     * as each maintains its own state.
     */
    fun createRenderer(): OffscreenCommandRenderer {
        return OffscreenCommandRenderer(parentScope, commands)
    }

    fun isEmpty() = commands.isEmpty()

    internal fun toText() = commands.toText()
}
fun OffscreenBuffer.isNotEmpty() = !isEmpty()

/** How many lines of text were generated within this offscreen buffer. */
val OffscreenBuffer.numLines get() = lineLengths.size

/**
 * A renderer tied to an [OffscreenBuffer] which allows the user to render one row of text at a time.
 */
class OffscreenCommandRenderer internal constructor(
    private val targetScope: RenderScope,
    private val commands: List<TerminalCommand>
) {
    private var commandIndex = 0
    private var lastState: SectionState? = null

    /** Returns true if there is at least one more row of text to render. */
    fun hasNextRow(): Boolean = (commandIndex < commands.size)

    /**
     * Render a single row of commands, up to but *excluding* the row's newline.
     *
     * The reason to render a single row instead of all at once is because it's expected that you are wrapping
     * this content with some external output, e.g. an outer border around some inner text. Here, you'd render the
     * left wall of the border, for example, a row of content, the right wall of the border, etc.
     *
     * Newlines are excluded for the same reason -- because it's expected the caller might add some trailing decoration
     * after the row text and will be handling the newline itself anyway.
     */
    fun renderNextRow(): Boolean {
        if (!hasNextRow()) {
            return false
        }

        targetScope.scopedState {
            lastState?.let { targetScope.state = it }
            while (commandIndex < commands.size) {
                if (commands[commandIndex] === NEWLINE_COMMAND) {
                    ++commandIndex
                    break
                }

                if (commands[commandIndex] !== NEWLINE_COMMAND) {
                    targetScope.applyCommand(commands[commandIndex])
                    ++commandIndex
                }
            }
            lastState = targetScope.state
        }

        return true
    }
}

/**
 * An offscreen block lets you create a temporary internal section that doesn't render until you're ready for it to.
 *
 * This method returns an intermediate object which can then render a row at a time using a buffer renderer:
 *
 * ```
 * val buffer = offscreen { ... }
 * val renderer = buffer.createRenderer()
 * while (true) {
 *   renderer.renderNextRow() // Adds offscreen commands to our current render scope
 *   textLine()
 * }
 * ```
 *
 * While the above code is useless as-is (it's identical to rendering directly to the current render scope), you can
 * query an offscreen buffer in order to do more useful things.
 *
 * For example, here we right-align a block of text:
 *
 * ```
 * val buffer = offscreen { ... }
 * val maxWidth = buffer.lineLengths.maxOrNull() ?: 0
 * val renderer = buffer.createRenderer()
 * content.lineLengths.forEach { lineLength ->
 *   repeat(maxWidth - lineLength) { text(' ') }
 *   renderer.renderNextRow()
 *   textLine()
 * }
 * ```
 *
 * *Note: Instead of doing that yourself, you may use the [justified] helper method instead.*
 *
 * The initial state for the offscreen buffer inherits the parent state, so that this would render green the first time
 * and blue the second:
 *
 * ```
 * val buffer = offscreen { textLine("Inherits color from parent") }
 *
 * green()
 * buffer.createRenderer().let { renderer1 ->
 *   ... render all rows, will be green ...
 * }
 *
 * blue()
 * buffer.createRenderer().let { renderer2 ->
 *   ... render all rows, will be blue ...
 * }
 * ```
 *
 * Despite initially inheriting the text effect state from the parent scope, you can also modify text effects *inside*
 * an offscreen render block, which would then remember that for that point on without affecting the parent state:
 *
 * ```
 * val buffer = offscreen {
 *   textLine("Red (from parent)")
 *   blue()
 *   textLine("Blue")
 *   textLine("Blue again")
 * }
 *
 * red()
 * val renderer = buffer.createRenderer()
 * while (renderer.hasNextRow()) {
 *   text("Red "); renderer.renderNextRow(); textLine( "Red")
 * }
 * ```
 *
 * This lets you do whatever you want within an offscreen render block without worrying about it leaking into the
 * parent scope.
 *
 * See also: [justified], [bordered]
 */
fun RenderScope.offscreen(render: OffscreenRenderScope.() -> Unit): OffscreenBuffer {
    return offscreen(Int.MAX_VALUE, render)
}

fun RenderScope.offscreen(maxWidth: Int, render: OffscreenRenderScope.() -> Unit): OffscreenBuffer {
    return OffscreenBuffer(this, maxWidth, render)
}
