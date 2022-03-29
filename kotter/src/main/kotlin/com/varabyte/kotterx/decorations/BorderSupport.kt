package com.varabyte.kotterx.decorations

import com.varabyte.kotter.foundation.render.offscreen
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.render.RenderScope

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
 */
fun RenderScope.bordered(
    borderCharacters: BorderCharacters = BorderCharacters.BOX_THIN,
    paddingLeftRight: Int = 0,
    paddingTopBottom: Int = 0,
    render: RenderScope.() -> Unit
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