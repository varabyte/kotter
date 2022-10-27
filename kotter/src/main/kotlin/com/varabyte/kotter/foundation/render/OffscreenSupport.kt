package com.varabyte.kotter.foundation.render

import com.varabyte.kotter.runtime.SectionState
import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.kotter.runtime.internal.ansi.commands.RESET_COMMAND
import com.varabyte.kotter.runtime.internal.text.lineLengths
import com.varabyte.kotter.runtime.internal.text.toRawText
import com.varabyte.kotter.runtime.render.OneShotRenderScope
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotter.runtime.render.Renderer

/**
 * A [RenderScope] used for the [offscreen] method.
 *
 * While it seems unnecessary to create an empty class like this, this can be useful if library authors want to provide
 * extension methods that only apply to `offscreen` scopes.
 */
class OffscreenRenderScope(renderer: Renderer<OffscreenRenderScope>): OneShotRenderScope(renderer)

/**
 * An internal buffer that stores commands without actively rendering them.
 *
 * As a user, you won't create one yourself, but you'll use [offscreen] to do so.
 *
 * This class itself is inert, but you can use [createRenderer] to create a class that can apply its commands to the
 * current render scope.
 *
 * @property parentScope The [RenderScope] this buffer is tied to. This parameter is exposed for testing.
 */
class OffscreenBuffer(internal val parentScope: RenderScope, render: OffscreenRenderScope.() -> Unit) {
    private val commands = run {
        val offscreenRenderer = Renderer<OffscreenRenderScope>(parentScope.renderer.session) { OffscreenRenderScope(it) }.apply {
            render(render)
        }
        // The renderer normally makes sure that a command block ends with a trailing newline and a state reset, but we
        // don't need those in an offscreen buffer.
        // 1) The final newline shouldn't leak into the output. For example,
        // `offscreen { lines.forEach { textLine(it } }` shouldn't create a trailing single empty line.
        // 2) `CommandRenderer` below handles its own state cleanup logic
        // Note: The order of the newline and reset depend on if the final row ended with a newline or not. Either way,
        // we want to remove both of them!
        assert(offscreenRenderer.commands.takeLast(2).containsAll(listOf(NEWLINE_COMMAND, RESET_COMMAND)))
        offscreenRenderer.commands.dropLast(2)
    }

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

    internal fun toRawText() = commands.toRawText()
}

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

    fun hasNextRow(): Boolean = (commandIndex < commands.size)

    /**
     * Render a single row of commands, up to but *excluding* the row's newline.
     *
     * Newlines are excluded because it's expected you're using an offscreen buffer to render text within some wrapped
     * context (like a border or padding spaces).
     *
     * The reason to render a single row instead of all at once is because it's expected that you are wrapping
     * this content with some external output, e.g. an outer border around some inner text. Here, you'd render the
     * left wall of the border, for example, a row of content, the right wall of the border, etc.
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
 * Note that the initial state for the offscreen buffer uses the parent state, so that this would render blue the
 * first time and green the second:
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
 * This method is particularly useful for layout purposes, where you can calculate the size of what the render area will
 * be, e.g. to wrap things with a border.
 */
fun RenderScope.offscreen(render: OffscreenRenderScope.() -> Unit): OffscreenBuffer {
    return OffscreenBuffer(this, render)
}