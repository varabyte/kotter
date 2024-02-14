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

        /**
         * A blank border, where cell elements float separated by space.
         */
        val INVISIBLE get() = GridCharacters(' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ')
    }
}

/**
 * A column specification for a grid.
 */
class Cols private constructor(internal vararg val specs: Spec) {
    init {
        fun assertPositive(value: Int?, name: String) {
            if (value != null) require(value > 0) { "$name must be positive" }
        }

        fun assertOrdered(min: Int?, max: Int?, minName: String, maxName: String) {
            if (min != null && max != null) require(min <= max) { "$minName must be less than or equal to $maxName" }
        }

        require(specs.isNotEmpty()) { "Must declare at least one column" }
        specs.forEach { spec ->
            when (spec) {
                is Spec.Fit -> {
                    assertPositive(spec.minWidth, "minWidth")
                    assertPositive(spec.maxWidth, "maxWidth")
                    assertOrdered(spec.minWidth, spec.maxWidth, "minWidth", "maxWidth")
                }

                is Spec.Fixed -> assertPositive(spec.width, "width")
                is Spec.Star -> {
                    assertPositive(spec.ratio, "ratio")
                    assertPositive(spec.minWidth, "minWidth")
                    assertPositive(spec.maxWidth, "maxWidth")
                    assertOrdered(spec.minWidth, spec.maxWidth, "minWidth", "maxWidth")
                }
            }
        }
    }

    constructor(vararg widths: Int) : this(*widths.map { Spec.Fixed(it) }.toTypedArray())

    internal sealed class Spec(val justification: Justification?) {
        class Fit(justification: Justification? = null, val minWidth: Int? = null, val maxWidth: Int? = null) :
            Spec(justification)

        class Fixed(val width: Int, justification: Justification? = null) : Spec(justification)

        class Star(
            val ratio: Int = 1,
            justification: Justification? = null,
            val minWidth: Int? = null,
            val maxWidth: Int? = null
        ) : Spec(justification)
    }

    class BuilderScope {
        private val specs = mutableListOf<Spec>()

        fun fit(justification: Justification? = null, minWidth: Int? = null, maxWidth: Int? = null) {
            specs.add(Spec.Fit(justification, minWidth, maxWidth))
        }

        fun fixed(width: Int, justification: Justification? = null) {
            specs.add(Spec.Fixed(width, justification))
        }

        fun star(ratio: Int = 1, justification: Justification? = null, minWidth: Int? = null, maxWidth: Int? = null) {
            specs.add(Spec.Star(ratio, justification, minWidth, maxWidth))
        }

        fun build() = Cols(*specs.toTypedArray())
    }

    companion object {
        /**
         * Create a column spec where all columns are the same width.
         */
        fun uniform(count: Int, width: Int) = Cols(*IntArray(count) { width })

        /**
         * Convenience method for constructing a [Cols] instance using a builder pattern.
         */
        operator fun invoke(builder: BuilderScope.() -> Unit): Cols {
            val scope = BuilderScope()
            scope.builder()
            return scope.build()
        }
    }
}

private typealias OffscreenRenderBlock = (OffscreenRenderScope.() -> Unit)

class GridScope(private val cols: Cols) {
    internal class Data(
        val cellBlock: OffscreenRenderBlock,
        var justificationOverride: Justification?
    )

    private val _cellData = mutableListOf<Data?>()
    internal val cellData: List<Data?> = _cellData

    private var nextRow = 0
    private var nextCol = 0

    private fun cellIndex(row: Int, col: Int) = row * cols.specs.size + col

    /**
     * Declare a grid cell.
     *
     * It automatically lays out the cell using a fairly straightforward flow algorithm. You can use `cell(row, col)` if
     * you need more control about where it shows up.
     *
     * Note that the cell will always be positioned after the last cell in the entire grid. That is, if you have a
     * two-column grid, then add a cell explicitly at (2, 0) and a second at (0, 0), calling `cell` again will add a
     * new cell at (2, 1), not (0, 1).
     *
     * See the header docs for [grid] for more information.
     */
    fun cell(justification: Justification? = null, render: OffscreenRenderScope.() -> Unit = {}) {
        val cellIndex = cellIndex(nextRow, nextCol)
        while (cellIndex >= _cellData.size) {
            _cellData.add(null)
        }

        require(cellIndex > _cellData.lastIndex || _cellData[cellIndex] == null) {
            "Attempting to declare a grid cell in a spot that's already taken: ($nextRow, $nextCol)"
        }

        _cellData[cellIndex] = Data(render, justification)

        nextRow = _cellData.size / cols.specs.size
        nextCol = _cellData.size % cols.specs.size
    }

    /**
     * Declare a grid cell at a specific row and column.
     *
     * Note that this will throw an exception if that cell position is already filled.
     *
     * See the header docs for [grid] for more information.
     */
    fun cell(row: Int, col: Int, justification: Justification? = null, render: OffscreenRenderScope.() -> Unit = {}) {
        nextRow = row
        nextCol = col
        cell(justification, render)
    }
}

/**
 * Declare a grid of cells.
 *
 * With grids, you define columns explicitly; rows are added automatically as needed.
 *
 * Here, we create a grid of two 10-width columns:
 * ```
 * grid(Cols.uniform(2, 10), paddingLeftRight = 1) {
 *   cell { textLine("Cell 1a") }
 *   cell { textLine("Cell 1b") }
 *   cell { textLine("Cell 2a") }
 *   cell { textLine("Cell 2b") }
 * }
 * ```
 *
 * which renders:
 * ```
 * +----------+----------+
 * | Cell 1a  | Cell 1b  |
 * +----------+----------+
 * | Cell 2a  | Cell 2b  |
 * +----------+----------+
 * ```
 *
 * It's worth noting that the width of the grid above will actually be 23, not 20, because of the border characters,
 * which are not included in the cell width calculations.
 *
 * You can also specify the specific row and column you want a cell to apply to:
 * ```
 * grid(Cols.uniform(2, 10), paddingLeftRight = 1) {
 *   cell { textLine("Cell 1a") }
 *   cell(row = 1, col = 1) { textLine("Cell 2b") }
 * }
 * ```
 *
 * which renders:
 * ```
 * +----------+----------+
 * | Cell 1a  |          |
 * +----------+----------+
 * |          | Cell 2b  |
 * +----------+----------+
 * ```
 *
 * If the contents of a cell can't fit, it will insert newlines:
 * ```
 * grid(Cols(6)) {
 *   cell { textLine("Hello grid!") }
 * }
 * ```
 *
 * which renders:
 * ```
 * +------+
 * |Hello |
 * |grid! |
 * +------+
 * ```
 *
 * @param cols The column specification for the grid.
 * @param targetWidth The target width to render this grid at. This value has no meaning if there are no star-sized
 *   columns in this grid; however, if there is at least one, the user should set it or else all star-sized columns will
 *   be set to 1. It's worth noting that this target width doesn't include padding or space for border walls, so the
 *   final rendered size of the grid will be larger than this value.
 * @param characters The characters used to render the grid's border.
 * @param paddingLeftRight If set, adds some additional padding at the start and end of every cell. This space will be
 *   added in addition to the width of the cell, so e.g. `Cols(1)` with `paddingLeftRight = 2` will actually render as
 *   width 5.
 * @param paddingTopBottom If set, adds some extra blank lines to the top and bottom of each cell.
 * @param defaultJustification The default justification to use for all cells. You can override this value on a case-by
 *   case basis by passing in a value to the `justification` parameter of `cell` call, or for the entire column by
 *   passing in a justification value to the [Cols.Spec] class.
 * @param maxCellHeight The maximum height to allow cells to grow to, which can happen if a cell contains many newlines
 *   or a "*" column gets squished a lot, forcing newlines to be inserted.
 */
fun RenderScope.grid(
    cols: Cols,
    targetWidth: Int? = null,
    characters: GridCharacters = GridCharacters.ASCII,
    paddingLeftRight: Int = 0,
    paddingTopBottom: Int = 0,
    defaultJustification: Justification = Justification.LEFT,
    maxCellHeight: Int = Int.MAX_VALUE,
    render: GridScope.() -> Unit
) {
    val gridScope = GridScope(cols)
    gridScope.render()

    val colWidthsWithoutPadding = run {
        val colMinWidths = cols.specs.mapIndexed { x, spec ->
            when (spec) {
                is Cols.Spec.Fit -> {
                    var maxWidth = 0
                    var cellIndex = x
                    while (cellIndex < gridScope.cellData.size) {
                        val cellBlock = gridScope.cellData.getOrNull(cellIndex)?.cellBlock
                        if (cellBlock != null) {
                            val cellRenderer = this.offscreen(Int.MAX_VALUE, cellBlock)
                            cellRenderer.lineLengths.maxOrNull()?.let { cellWidth ->
                                maxWidth = maxOf(maxWidth, cellWidth)
                            }
                        }
                        cellIndex += cols.specs.size
                    }

                    maxWidth.coerceIn(spec.minWidth ?: 0, spec.maxWidth ?: Int.MAX_VALUE)
                }

                is Cols.Spec.Fixed -> spec.width
                is Cols.Spec.Star -> 0 // Set to 0, not spec.minWidth, for the `widthForStarCols` calculation to work
            }
        }

        val starRatioSum = cols.specs.asSequence().mapNotNull { spec ->
            if (spec is Cols.Spec.Star) spec.ratio else null
        }.sum()
        val widthForStarCols = targetWidth
            ?.let { it - colMinWidths.sum() }
            ?.takeIf { it >= 0 } ?: 0

        cols.specs.mapIndexed { i, spec ->
            when (spec) {
                is Cols.Spec.Fit -> colMinWidths[i]
                is Cols.Spec.Fixed -> spec.width
                is Cols.Spec.Star -> ((spec.ratio * widthForStarCols) / starRatioSum).coerceIn(
                    spec.minWidth ?: 1,
                    spec.maxWidth ?: Int.MAX_VALUE
                )
            }
        }
    }
    val colWidthsWithPadding = colWidthsWithoutPadding.map { it + paddingLeftRight * 2 }

    // Render top
    text(characters.topLeft)
    colWidthsWithPadding.forEachIndexed { i, width ->
        if (i > 0) text(characters.topCross)
        text(characters.horiz.toString().repeat(width))
    }
    textLine(characters.topRight)

    val rowCount =
        gridScope.cellData.size / cols.specs.size + if (gridScope.cellData.size % cols.specs.size > 0) 1 else 0
    val lastRowIndex = rowCount - 1

    val cellBuffers = gridScope.cellData.mapIndexed { i, data ->
        val x = i % cols.specs.size
        if (data != null) {
            this.offscreen(colWidthsWithoutPadding[x], data.cellBlock)
        } else null
    }

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
            for (colWidth in colWidthsWithPadding) {
                text(characters.vert)
                text(" ".repeat(colWidth))
            }
            textLine(characters.vert)
        }

        repeat(paddingTopBottom) {
            renderEmptyRow()
        }

        val renderers = cellBuffers
            .subList(y * cols.specs.size, min((y + 1) * cols.specs.size, gridScope.cellData.size))
            .map { it?.createRenderer() }

        if (renderers.all { it == null }) {
            // This happens if users used `cell(row, col)` to completely skip one or more rows
            renderEmptyRow()
        } else {
            var lineIndex = 0
            while (lineIndex < maxCellHeight && renderers.any { it?.hasNextRow() == true }) {
                for (x in cols.specs.indices) {
                    text(characters.vert)

                    val buffer = cellBuffers.getOrNull(y * cols.specs.size + x)?.takeIf { it.isNotEmpty() }
                    if (buffer != null) {
                        val extraSpace = colWidthsWithoutPadding[x] - buffer.lineLengths.getOrElse(lineIndex) { 0 }
                        val renderer = renderers.getOrNull(x)
                        val justification =
                            gridScope.cellData[y * cols.specs.size + x]?.justificationOverride
                                ?: cols.specs[x].justification
                                ?: defaultJustification
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
                        repeat(colWidthsWithPadding[x]) { text(" ") }
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
            colWidthsWithPadding.forEachIndexed { i, width ->
                if (i > 0) text(characters.cross)
                text(characters.horiz.toString().repeat(width))
            }
            textLine(characters.rightCross)
        }
    }

    // Render bottom
    text(characters.botLeft)
    colWidthsWithPadding.forEachIndexed { i, width ->
        if (i > 0) text(characters.botCross)
        text(characters.horiz.toString().repeat(width))
    }
    textLine(characters.botRight)
}
