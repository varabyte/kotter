package com.varabyte.kotterx.grid

import com.varabyte.kotter.foundation.render.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotter.runtime.render.OffscreenRenderScope
import com.varabyte.kotterx.decorations.BorderCharacters.Companion.BOX_THIN
import com.varabyte.kotterx.text.*
import kotlin.math.min

class GridCharacters(
    val horiz: Char,
    val vert: Char,
    val topLeft: Char,
    val topCross: Char,
    val topRight: Char,
    val leftCross: Char,
    val cross: Char,
    val rightCross: Char,
    val botLeft: Char,
    val botCross: Char,
    val botRight: Char,
) {
    companion object {
        /**
         * Grid border using basic ASCII characters guaranteed to be present in every environment.
         *
         * ```
         * +-+-+
         * | | |
         * +-+-+
         * | | |
         * +-+-+
         * ```
         */
        val ASCII get() = GridCharacters('-', '|', '+', '+', '+', '+', '+', '+', '+', '+', '+')

        /**
         * Grid border using fairly standard unicode box characters.
         *
         * ```
         * ┌─┬─┐
         * │ │ │
         * ├─┼─┤
         * │ │ │
         * └─┴─┘
         * ```
         */

        val BOX_THIN get() = GridCharacters('─', '│', '┌', '┬', '┐', '├', '┼', '┤', '└', '┴', '┘')

        /**
         * Like [BOX_THIN] but with a double-border.
         *
         * ```
         * ╔═╦═╗
         * ║ ║ ║
         * ╠═╬═╣
         * ║ ║ ║
         * ╚═╩═╝
         * ```
         */
        val BOX_DOUBLE get() = GridCharacters('═', '║', '╔', '╦', '╗', '╠', '╬', '╣', '╚', '╩', '╝')

        /**
         * An elegant, sleek, curved border for the sophisticated user. 🧐
         *
         * ```
         * ╭─┬─╮
         * │ │ │
         * ├─┼─┤
         * │ │ │
         * ╰─┴─╯
         * ```
         */
        val CURVED get() = GridCharacters('─', '│', '╭', '┬', '╮', '├', '┼', '┤', '╰', '┴', '╯')

        val INVISIBLE get() = GridCharacters(' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ')
    }
}

class Cols(vararg val widths: Int) {
    private sealed class ColSpec {
        class Fixed(val width: Int) : ColSpec()
        class Star(val ratio: Int) : ColSpec()
    }

    companion object {
        fun uniform(count: Int, width: Int) = Cols(*IntArray(count) { width })

        /**
         * Parse a string of comma-separated column widths or "*"s.
         *
         * Star-sizing means that the column will take up the remaining space in the grid. If multiple different star
         * sections exist, they will be divided up evenly based on their ratio. For example, with "2*, *", the first
         * column will be twice as wide as the second.
         *
         * You can mix and match types. So "2*, 20, *" with a target width of 50 will result in columns of 20, 20, and
         * 10.
         *
         * Note that the target width is used for star sizing only. If the target width is 20 and the columns are
         * "10, 20, 30", then you'll have a table of width 60!
         */
        fun fromStr(str: String, targetWidth: Int? = null): Cols {
            fun invalidPartMessage(part: String) = "Invalid column spec: $part"
            val specs = str
                .split(",")
                .map { it.trim() }
                .map { part ->
                    if (part.endsWith("*")) {
                        val ratio = part.dropLast(1).let {
                            if (it.isEmpty()) 1 else it.toIntOrNull()
                        }
                        require(ratio != null) { invalidPartMessage(part) }
                        ColSpec.Star(ratio)
                    } else {
                        val width = part.toIntOrNull()
                        require(width != null) { invalidPartMessage(part) }
                        ColSpec.Fixed(width)
                    }
                }

            val starTotal = specs.sumOf { if (it is ColSpec.Star) it.ratio else 0 }
            require(targetWidth != null || starTotal == 0) { "You must pass in a target width to `Cols.fromStr` if you use star sizing (col str = $str)" }
            val finalWidth = targetWidth?.let {
                it - specs.sumOf { spec -> if (spec is ColSpec.Fixed) spec.width else 0 }
            } ?: 0
            return Cols(*specs.map { spec ->
                when (spec) {
                    is ColSpec.Fixed -> spec.width
                    is ColSpec.Star -> (finalWidth * spec.ratio) / starTotal
                }
            }.toIntArray())
        }
    }
}

class GridScope(internal val renderScope: RenderScope, private val paddingLeftRight: Int, private val cols: Cols) {
    private val _cellBuffers = mutableListOf<OffscreenBuffer?>()
    internal val cellBuffers: List<OffscreenBuffer?> = _cellBuffers
    private val _justificationOverrides = mutableMapOf<OffscreenBuffer, Justification>()
    internal val justificationOverrides: Map<OffscreenBuffer, Justification> = _justificationOverrides

    private var nextRow = 0
    private var nextCol = 0

    private fun cellIndex(row: Int, col: Int) = row * cols.widths.size + col

    fun cell(justification: Justification? = null, render: OffscreenRenderScope.() -> Unit = {}) {
        val cellIndex = cellIndex(nextRow, nextCol)
        while (cellIndex >= cellBuffers.size) {
            _cellBuffers.add(null)
        }

        require(cellIndex > cellBuffers.lastIndex || cellBuffers[cellIndex] == null) {
            "Attempting to declare a grid cell in a spot that's already taken: ($nextRow, $nextCol)"
        }

        _cellBuffers[cellIndex] = renderScope.offscreen(cols.widths[nextCol] - paddingLeftRight * 2, render)
            .also { buffer ->
                if (justification != null) {
                    _justificationOverrides[buffer] = justification
                }
            }

        nextRow = cellBuffers.size / cols.widths.size
        nextCol = cellBuffers.size % cols.widths.size
    }

    fun cell(row: Int, col: Int, justification: Justification? = null, render: OffscreenRenderScope.() -> Unit = {}) {
        nextRow = row
        nextCol = col
        cell(justification, render)
    }
}

fun RenderScope.grid(
    cols: Cols,
    characters: GridCharacters = GridCharacters.ASCII,
    paddingLeftRight: Int = 0,
    paddingTopBottom: Int = 0,
    defaultJustification: Justification = Justification.LEFT,
    render: GridScope.() -> Unit
) {

    require(paddingLeftRight < cols.widths.min()) {
        "Invalid LeftRight Padding or Column Spec: padding can't be wider than columns. " +
                "Min Column: ${cols.widths.min()}, LeftRight Padding: $paddingLeftRight"
    }

    val gridScope = GridScope(this, paddingLeftRight, cols)
    gridScope.render()

    // Render top
    text(characters.topLeft)
    cols.widths.forEachIndexed { i, width ->
        if (i > 0) text(characters.topCross)
        text(characters.horiz.toString().repeat(width))
    }
    textLine(characters.topRight)

    val rowCount =
        gridScope.cellBuffers.size / cols.widths.size + if (gridScope.cellBuffers.size % cols.widths.size > 0) 1 else 0
    val lastRowIndex = rowCount - 1

    for (y in 0 until rowCount) {
        // Renderers can generate multiple lines. We need to render each line of each cell one at a time, so we create
        // them up front and then loop through each renderer multiple times until all lines are consumed across all
        // renderers.
        //
        // For example:
        // | Single line | Multi | Multi |
        // |             | line  | multi |
        // |             |       | line  |
        //
        // ^ We need to do three passes to consume all renderers.

        fun renderEmptyRow() {
            for (x in 0 until cols.widths.size) {
                text(characters.vert)
                text(" ".repeat(cols.widths[x]))
            }
            textLine(characters.vert)
        }

        repeat(paddingTopBottom) {
            renderEmptyRow()
        }

        val renderers = gridScope.cellBuffers
            .subList(y * cols.widths.size, min((y + 1) * cols.widths.size, gridScope.cellBuffers.size))
            .map { it?.createRenderer() }

        if (renderers.all { it == null }) {
            // This happens if users used `cell(row, col)` to completely skip one or more rows
            renderEmptyRow()
        } else {
            var lineIndex = 0
            while (renderers.any { it?.hasNextRow() == true }) {
                for (x in 0 until cols.widths.size) {
                    text(characters.vert)

                    val buffer = gridScope.cellBuffers.getOrNull(y * cols.widths.size + x)?.takeIf { it.isNotEmpty() }
                    val cellWidth = cols.widths[x] - paddingLeftRight * 2
                    if (buffer != null) {
                        val extraSpace = cellWidth - buffer.lineLengths.getOrElse(lineIndex) { 0 }
                        val renderer = renderers.getOrNull(x)
                        val justification = gridScope.justificationOverrides[buffer] ?: defaultJustification
                        repeat(paddingLeftRight) { text(" ") }
                        when (justification) {
                            Justification.LEFT -> {
                                renderer?.renderNextRow()
                                repeat(extraSpace) { text(" ") }
                            }

                            Justification.CENTER -> {
                                val leftSpace = extraSpace / 2
                                val rightSpace = extraSpace - leftSpace
                                repeat(leftSpace) { text(" ") }
                                renderer?.renderNextRow()
                                repeat(rightSpace) { text(" ") }
                            }

                            Justification.RIGHT -> {
                                repeat(extraSpace) { text(" ") }
                                renderer?.renderNextRow()
                            }
                        }
                        repeat(paddingLeftRight) { text(" ") }
                    } else {
                        repeat(cellWidth + paddingLeftRight * 2) { text(" ") }
                    }
                }
                textLine(characters.vert)
                lineIndex++
            }
        }

        repeat(paddingTopBottom) {
            renderEmptyRow()
        }

        if (y < lastRowIndex) {
            text(characters.leftCross)
            cols.widths.forEachIndexed { i, width ->
                if (i > 0) text(characters.cross)
                text(characters.horiz.toString().repeat(width))
            }
            textLine(characters.rightCross)
        }
    }

    // Render bottom
    text(characters.botLeft)
    cols.widths.forEachIndexed { i, width ->
        if (i > 0) text(characters.botCross)
        text(characters.horiz.toString().repeat(width))
    }
    textLine(characters.botRight)
}
