package com.varabyte.kotterx.text

import com.varabyte.kotter.foundation.render.OffscreenRenderScope
import com.varabyte.kotter.foundation.render.offscreen
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.render.RenderScope

enum class Justification {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * Adding spacing around lines to support justification across a block of text.
 *
 * @param padRight If true, append spaces at the end of the text to fill out any remaining calculated space. You
 *   probably want to do this if you're decorating the text somehow, e.g. calling `text` right after each line. You can
 *   otherwise disable this feature if not, and shave a few blank spaces off of your output text.
 */
fun RenderScope.justified(
    justification: Justification,
    padRight: Boolean = justification != Justification.RIGHT,
    render: OffscreenRenderScope.() -> Unit
) {
    val content = offscreen(render)
    val maxWidth = (content.lineLengths.maxOrNull() ?: 0)

    val renderer = content.createRenderer()
    for (i in content.lineLengths.indices) {
        val (leftPad, rightPad) = when (justification) {
            Justification.LEFT -> 0 to maxWidth - content.lineLengths[i]
            Justification.CENTER -> {
                val spaceToDivideIntoTwo = maxWidth - content.lineLengths[i]
                val firstHalf = spaceToDivideIntoTwo / 2
                val secondHalf = spaceToDivideIntoTwo - firstHalf
                (firstHalf to secondHalf)
            }
            else -> maxWidth - content.lineLengths[i] to 0
        }
        repeat(leftPad) { text(" ") }
        renderer.renderNextRow()
        if (padRight) {
            repeat(rightPad) { text(" ") }
        }
        textLine()
    }
}