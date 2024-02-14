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
class Cols(vararg val specs: Spec) {
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

    sealed class Spec(val justification: Justification?) {
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

    companion object {
        /**
         * Create a column spec where all columns are the same width.
         */
        fun uniform(count: Int, width: Int) = Cols(*IntArray(count) { width })

        /**
         * Parse a string which represents [Cols.Spec] values and use it to create a [Cols] instance.
         *
         * In other words, this is an efficient (but less type-safe) way to create a list of grid column specs, with
         * concise syntax for fixed-, fit-, and star-sized columns.
         *
         * For example: `Cols.fromStr("fit, 2*, 20, *")`
         *
         * Fit-sizing means that the column will take up exactly the space it needs to contain the largest item in that
         * column. Use the "fit" keyword to specify this.
         *
         * Star-sizing means that the column will take up the remaining space in the grid. If multiple different star
         * sections exist, they will be divided up evenly based on their ratio. For example, with "2*, *", the first
         * column will be twice as wide as the second.
         *
         * And fixed-sizing means that the column will take up exactly the specified width. Use a raw integer value to
         * specify this.
         *
         * You can mix and match types. So "fit, 2*, 20, *" means: subtract the fit size of the first column and the
         * fixed size of the third column, then divide any remaining space up between the star-sized columns. Let's say
         * the target width of the grid is 50, and the fit column ended up getting calculated as 6, then the final sizes
         * would be: [6, 16, 20, 8].
         *
         * Additional properties can also be specified, using a `key:value` syntax. For example, `fit min:5 max:10`
         * means calculate a fit value for this column, but go no lower than 5 and no higher than 10. Fit and star-sized
         * columns can both have min- and max-width properties. All columns can specify a `just` property which can be
         * set to `left`, `center`, or `right`.
         *
         * | Name | Values | Applies to |
         * | ---- | ------ | ---------- |
         * | min  | int    | fit, star   |
         * | max  | int    | fit, star   |
         * | just | left, center, right | all |
         *
         * For example: `Cols.fromStr("fit min:5, 20 just: center, * max: 30")`
         */
        fun fromStr(str: String): Cols {
            fun invalidPartMessage(part: String, extra: String? = null) =
                "Invalid column spec: $part" + (extra?.let { " ($it)" } ?: "")

            class ParsedPart(val value: String, val properties: Map<String, String>) {
                private val part get() = "$value ${properties.entries.joinToString(" ") { (k, v) -> "$k:$v" }}"

                val maxWidth = properties["max"]?.toIntOrNull()
                val minWidth = properties["min"]?.toIntOrNull()
                val justification = properties["just"]?.let { justStr ->
                    Justification.values().find { it.name.equals(justStr, ignoreCase = true) }
                        ?: error(invalidPartMessage(
                            part,
                            "Invalid justification value \"$justStr\", should be one of [${
                                Justification.values().joinToString(", ") { it.name.lowercase() }
                            }]"
                        ))
                }
            }

            fun parsePart(part: String): ParsedPart {
                val parts = part.split(" ")
                val value = parts.first()
                val properties = parts.drop(1).map { it.split(":") }.associate { it[0] to it[1] }
                return ParsedPart(value, properties)
            }

            val specs: List<Spec> = str
                .split(",")
                .map { it.trim() }
                .map { part ->
                    val parsedPart = parsePart(part)

                    when {
                        parsedPart.value.equals("fit", ignoreCase = true) -> {
                            Spec.Fit(
                                parsedPart.justification,
                                parsedPart.minWidth,
                                parsedPart.maxWidth
                            )
                        }

                        parsedPart.value.endsWith("*") -> {
                            val ratio = parsedPart.value.dropLast(1).let {
                                if (it.isEmpty()) 1 else it.toIntOrNull()
                            }
                            require(ratio != null) { invalidPartMessage(part, "Invalid star size") }
                            Spec.Star(
                                ratio,
                                parsedPart.justification,
                                parsedPart.minWidth,
                                parsedPart.maxWidth
                            )
                        }

                        else -> {
                            val width = parsedPart.value.toIntOrNull()
                            require(width != null && width > 0) {
                                invalidPartMessage(
                                    part,
                                    "Column width must be positive"
                                )
                            }
                            Spec.Fixed(width, parsedPart.justification)
                        }
                    }
                }

            return Cols(*specs.toTypedArray())
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
