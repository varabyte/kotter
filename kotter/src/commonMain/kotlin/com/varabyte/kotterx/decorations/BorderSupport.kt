package com.varabyte.kotterx.decorations

import com.varabyte.kotter.foundation.render.offscreen
import com.varabyte.kotter.foundation.text.addNewlinesIfNecessary
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.concurrent.createKey
import com.varabyte.kotter.runtime.render.OffscreenRenderScope
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotterx.decorations.BorderCharacters.Companion.Ascii
import com.varabyte.kotterx.decorations.BorderCharacters.Companion.BoxThin
import com.varabyte.kotterx.decorations.BorderCharacters.Companion.Curved

private const val SINGLETON_NAMING_CONVENTION_MESSAGE = "Name updated to reflect standard Kotlin naming conventions around singleton objects."

/**
 * Characters which collectively describe the look and feel of some border built out of text.
 *
 * A few borders are defined out of the box, such as [Ascii], [Curved], and others.
 */
class BorderCharacters(
    val topLeft: Char,
    val topRight: Char,
    val botLeft: Char,
    val botRight: Char,
    val horiz: Char,
    val vert: Char,
) {
    @Suppress("unused") // Deprecated properties are unused but necessary to keep around for now
    companion object {
        /**
         * Border using basic ASCII characters guaranteed to be present in every environment.
         *
         * ```
         * +-+
         * | |
         * +-+
         * ```
         */
        val Ascii by lazy { BorderCharacters('+', '+', '+', '+', '-', '|') }

        /**
         * Border using fairly standard unicode box characters.
         *
         * ```
         * ┌─┐
         * │ │
         * └─┘
         * ```
         */
        val BoxThin by lazy { BorderCharacters('┌', '┐', '└', '┘', '─', '│') }

        /**
         * Like [BoxThin] but with a double-border.
         *
         * ```
         * ╔═╗
         * ║ ║
         * ╚═╝
         * ```
         */
        val BoxDouble by lazy { BorderCharacters('╔', '╗', '╚', '╝', '═', '║') }

        /**
         * An elegant, sleek, curved border for the sophisticated user. 🧐
         *
         * ```
         * ╭─╮
         * │ │
         * ╰─╯
         * ```
         */
        val Curved by lazy { BorderCharacters('╭', '╮', '╰', '╯', '─', '│') }

        @Deprecated(SINGLETON_NAMING_CONVENTION_MESSAGE, replaceWith = ReplaceWith("Ascii"))
        val ASCII get() = Ascii
        @Deprecated(SINGLETON_NAMING_CONVENTION_MESSAGE, replaceWith = ReplaceWith("BoxThin"))
        val BOX_THIN get() = BoxThin
        @Deprecated(SINGLETON_NAMING_CONVENTION_MESSAGE, replaceWith = ReplaceWith("BoxDouble"))
        val BOX_DOUBLE get() = BoxDouble
        @Deprecated(SINGLETON_NAMING_CONVENTION_MESSAGE, replaceWith = ReplaceWith("Curved"))
        val CURVED get() = Curved
    }
}

private val DefaultBorderStyleKey = Session.Lifecycle.createKey<BorderCharacters>()

/**
 * The default border style that the [bordered] method will use if not explicitly set.
 */
var Session.Defaults.borderStyle: BorderCharacters
    get() = data[DefaultBorderStyleKey] ?: BoxThin
    set(value) {
        data[DefaultBorderStyleKey] = value
    }


/**
 * Automatically render a border around some inner content.
 *
 * @param borderCharacters The characters used to render the border.
 * @param paddingLeftRight If set, adds some additional padding at the start and end of every line.
 * @param paddingTopBottom If set, adds some newlines before and after the entire block of text.
 * @param render The render block that generates content (e.g. via `textLine`) which will be wrapped within a border.
 */
fun RenderScope.bordered(
    borderCharacters: BorderCharacters = section.session.defaults.borderStyle,
    paddingLeftRight: Int = 0,
    paddingTopBottom: Int = 0,
    render: OffscreenRenderScope.() -> Unit
) {
    addNewlinesIfNecessary(1)

    val content = offscreen(render)
    val maxWidth = (content.lineWidths.maxOrNull() ?: 0)
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
    for (i in content.lineWidths.indices) {
        text(borderCharacters.vert)
        text(" ".repeat(paddingLeftRight))
        renderer.renderNextRow()
        repeat(maxWidth - content.lineWidths[i]) { text(" ") }
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
