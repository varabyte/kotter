package com.varabyte.kotterx.text

import com.varabyte.kotter.runtime.render.OffscreenRenderScope
import com.varabyte.kotter.foundation.render.offscreen
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.render.RenderScope
import kotlin.math.max

/** [Text justification](https://en.wikipedia.org/wiki/Typographic_alignment). */
enum class Justification {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * Adds spacing around lines to support justification across a block of text.
 *
 * @param justification The [Justification] to apply to the inner text.
 * @param minWidth If specified, treat the block being justified as at least this many characters wide. This allows you
 *   to do something like: `justified(CENTER, minWidth = 10) { text("HI") }` to center the text `"HI"` with 4 spaces on
 *   each side. This value will be ignored if the text content being justified is already longer than this anyway.
 * @param padRight If true, append spaces at the end of the text to fill out any remaining calculated space. You
 *   probably want to do this if you're decorating the text somehow, e.g. calling `text` right after each line. You can
 *   otherwise disable this feature if not, and shave a few blank spaces off of your output text.
 * @param render The render block that generates content (e.g. via `textLine`) which will have its text justified.
 */
fun RenderScope.justified(
    justification: Justification,
    minWidth: Int = 0,
    padRight: Boolean = justification != Justification.RIGHT,
    render: OffscreenRenderScope.() -> Unit
) {
    val content = offscreen(render)
    val maxWidth = max(content.lineLengths.maxOrNull() ?: 0, minWidth)

    val renderer = content.createRenderer()
    content.lineLengths.forEach { lineLength ->
        val (leftPad, rightPad) = when (justification) {
            Justification.LEFT -> 0 to maxWidth - lineLength
            Justification.CENTER -> {
                val spaceToDivideIntoTwo = maxWidth - lineLength
                val firstHalf = spaceToDivideIntoTwo / 2
                val secondHalf = spaceToDivideIntoTwo - firstHalf
                (firstHalf to secondHalf)
            }
            else -> maxWidth - lineLength to 0
        }
        repeat(leftPad) { text(" ") }
        renderer.renderNextRow()
        if (padRight) {
            repeat(rightPad) { text(" ") }
        }
        textLine()
    }
}