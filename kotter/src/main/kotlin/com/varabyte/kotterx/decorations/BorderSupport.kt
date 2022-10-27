package com.varabyte.kotterx.decorations

import com.varabyte.kotter.foundation.render.OffscreenRenderScope
import com.varabyte.kotter.foundation.render.offscreen
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.render.RenderScope

/**
 * Characters which collectively describe the look and feel of some ASCII border.
 *
 * A few borders are defined out of the box, such as [ASCII], [CURVED], and others.
 */
class BorderCharacters(
    val topLeft: Char,
    val topRight: Char,
    val botLeft: Char,
    val botRight: Char,
    val horiz: Char,
    val vert: Char,
) {
    companion object {
        /**
         * Border using basic ASCII characters guaranteed to be present in every environment
         *
         * ```
         * +-+
         * | |
         * +-+
         * ```
         */
        val ASCII get() = BorderCharacters('+', '+', '+', '+', '-', '|')

        /**
         * Border using fairly standard unicode box characters.
         *
         * ```
         * ┌─┐
         * │ │
         * └─┘
         * ```
         */
        val BOX_THIN get() = BorderCharacters('┌', '┐', '└', '┘', '─', '│')

        /**
         * Like [BOX_THIN] but with a double-border.
         *
         * ```
         * ╔═╗
         * ║ ║
         * ╚═╝
         * ```
         */
        val BOX_DOUBLE get() = BorderCharacters('╔', '╗', '╚', '╝', '═', '║')

        /**
         * An elegant, sleek, curved border for the sophisticated user. 🧐
         *
         * ```
         * ╭─╮
         * │ │
         * ╰─╯
         * ```
         */
        val CURVED get() = BorderCharacters('╭', '╮', '╰', '╯', '─', '│')
    }
}

/**
 * Automatically render a border around some inner content.
 *
 * @param borderCharacters The characters used to render the border.
 * @param paddingLeftRight If set, adds some additional padding at the start and end of every line.
 * @param paddingTopBottom If set, adds some newlinse before and after the entire block of text.
 * @param render The render block that generates content (e.g. via `textLine`) which will be wrapped within a border.
 */
fun RenderScope.bordered(
    borderCharacters: BorderCharacters = BorderCharacters.BOX_THIN,
    paddingLeftRight: Int = 0,
    paddingTopBottom: Int = 0,
    render: OffscreenRenderScope.() -> Unit
) {
    val content = offscreen(render)
    val maxWidth = (content.lineLengths.maxOrNull() ?: 0)
    val maxWidthWithPadding = maxWidth + paddingLeftRight * 2

    text(borderCharacters.topLeft)
    borderCharacters.horiz.toString().repeat(maxWidthWithPadding).let { text(it) }
    textLine(borderCharacters.topRight)

    for (i in 0 until paddingTopBottom) {
        text(borderCharacters.vert)
        text(" ".repeat(maxWidthWithPadding))
        textLine(borderCharacters.vert)
    }

    val renderer = content.createRenderer()
    for (i in content.lineLengths.indices) {
        text(borderCharacters.vert)
        text(" ".repeat(paddingLeftRight))
        renderer.renderNextRow()
        repeat(maxWidth - content.lineLengths[i]) { text(" ") }
        text(" ".repeat(paddingLeftRight))
        textLine(borderCharacters.vert)
    }

    for (i in 0 until paddingTopBottom) {
        text(borderCharacters.vert)
        text(" ".repeat(maxWidthWithPadding))
        textLine(borderCharacters.vert)
    }

    text(borderCharacters.botLeft)
    borderCharacters.horiz.toString().repeat(maxWidthWithPadding).let { text(it) }
    textLine(borderCharacters.botRight)
}