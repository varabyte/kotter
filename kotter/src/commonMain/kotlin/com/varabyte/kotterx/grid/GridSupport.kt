package com.varabyte.kotterx.grid

import com.varabyte.kotter.foundation.render.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.concurrent.createKey
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotterx.grid.GridScope.*
import com.varabyte.kotterx.text.*
import com.varabyte.kotterx.util.collections.Indices
import com.varabyte.kotterx.util.collections.indicesOf
import kotlin.math.min

private const val SINGLETON_NAMING_CONVENTION_MESSAGE = "Name updated to reflect standard Kotlin naming conventions around singleton objects."

/**
 * A list of ASCII characters which define a grid's borders.
 */
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
    @Suppress("unused") // Deprecated properties are unused but necessary to keep around for now
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
        val Ascii by lazy { GridCharacters('-', '|', '+', '+', '+', '+', '+', '+', '+', '+', '+') }

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

        val BoxThin by lazy { GridCharacters('─', '│', '┌', '┬', '┐', '├', '┼', '┤', '└', '┴', '┘') }

        /**
         * Like [BoxThin] but with a double-border.
         *
         * ```
         * ╔═╦═╗
         * ║ ║ ║
         * ╠═╬═╣
         * ║ ║ ║
         * ╚═╩═╝
         * ```
         */
        val BoxDouble by lazy { GridCharacters('═', '║', '╔', '╦', '╗', '╠', '╬', '╣', '╚', '╩', '╝') }

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
        val Curved by lazy { GridCharacters('─', '│', '╭', '┬', '╮', '├', '┼', '┤', '╰', '┴', '╯') }

        /**
         * A blank border, where cell elements float separated by space.
         */
        val Invisible by lazy { GridCharacters(' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ') }

        @Deprecated(SINGLETON_NAMING_CONVENTION_MESSAGE, replaceWith = ReplaceWith("Ascii"))
        val ASCII get() = Ascii
        @Deprecated(SINGLETON_NAMING_CONVENTION_MESSAGE, replaceWith = ReplaceWith("BoxThin"))
        val BOX_THIN get() = BoxThin
        @Deprecated(SINGLETON_NAMING_CONVENTION_MESSAGE, replaceWith = ReplaceWith("BoxDouble"))
        val BOX_DOUBLE get() = BoxDouble
        @Deprecated(SINGLETON_NAMING_CONVENTION_MESSAGE, replaceWith = ReplaceWith("Curved"))
        val CURVED get() = Curved
        @Deprecated(SINGLETON_NAMING_CONVENTION_MESSAGE, replaceWith = ReplaceWith("Invisible"))
        val INVISIBLE get() = Invisible
    }
}

private val DefaultGridStyleKey = Session.Lifecycle.createKey<GridCharacters>()

/**
 * The default border style that the [grid] method will use if not explicitly set.
 */
var Session.Defaults.gridStyle: GridCharacters
    get() = data[DefaultGridStyleKey] ?: GridCharacters.BoxThin
    set(value) {
        data[DefaultGridStyleKey] = value
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

    /**
     * A convenience constructor for creating a [Cols] instance where all columns are fixed widths.
     */
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

    /**
     * Scope used as the receiver for the block when creating a grid column specification using the builder pattern.
     *
     * @see Cols.invoke
     */
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

        internal fun build() = Cols(*specs.toTypedArray())
    }

    companion object {
        /**
         * Create a column spec where all columns are the same width.
         */
        fun uniform(count: Int, width: Int) = Cols(*IntArray(count) { width })

        /**
         * Convenience method for constructing a [Cols] instance using a builder pattern.
         *
         * For example:
         * ```
         * Cols { fit(); fixed(10); star() }
         * ```
         *
         * @see BuilderScope
         */
        operator fun invoke(builder: BuilderScope.() -> Unit): Cols {
            val scope = BuilderScope()
            scope.builder()
            return scope.build()
        }
    }
}

/**
 * Simple metrics about the current cell being rendered.
 *
 * Use it like so:
 *
 * ```kotlin
 * cell { cellMetrics ->
 *   text(textMetrics.truncateToWidth(".....", cellMetrics.width, ellipsis = "…")
 * }
 * ```
 *
 * Note that a cell does an initial measurement pass where no bounds are set, at which point
 * [isMeasurementPass] will return true and [width] will return [Int.MAX_VALUE].
 */
class CellMetrics(
    val width: Int,
) {
    val isMeasurementPass: Boolean get() = width == Int.MAX_VALUE
    companion object {
        internal val Measuring = CellMetrics(Int.MAX_VALUE)
    }
}

private typealias CellRenderBlock = (OffscreenRenderScope.(CellMetrics) -> Unit)

/**
 * A scope used when constructing a grid, allowing the user to declare cells.
 */
class GridScope(private val cols: Cols) {
    internal sealed interface CellData {
        val justification: Justification?

        class Item(val cellBlock: CellRenderBlock, override val justification: Justification?, val width: Int, val height: Int) : CellData
        class Ptr(val target: Item, val offsetX: Int, val offsetY: Int) : CellData {
            override val justification = target.justification
        }
    }

    private val _cellData = mutableListOf<CellData?>()
    internal val cellData: List<CellData?> = _cellData

    /**
     * The next row that a cell will be placed into, assuming the user doesn't specify it explicitly.
     *
     * This value is exposed so that a user can write:
     * ```
     * cell(row = nextEmptyCellRow + 1)
     * ```
     * to skip adding elements to the current row and instead start on the next row.
     *
     * @see nextEmptyCellCol
     */
    var nextEmptyCellRow = 0
        private set

    /**
     * The next column that a cell will be placed into, assuming the user doesn't specify it explicitly.
     *
     * @see nextEmptyCellRow
     */
    var nextEmptyCellCol = 0
        private set

    internal fun cellIndex(row: Int, col: Int) = row * cols.specs.size + col

    /**
     * Declare a grid cell.
     *
     * If row and/or column aren't specified, the cell is automatically placed using a fairly straightforward flow
     * algorithm (next available column on the current row, or first column of the next row if at the end).
     *
     * You can specify the row and column values if you need more control about where it shows up.
     *
     * If row and cell aren't specified, the next cell will always be placed into the next available empty cell.
     * That is, if you have a two-column grid, then add a cell explicitly at (2, 0) and a second at (0, 0), calling
     * `cell` again will add a new cell at (0, 1). If you keep calling `cell`, it will skip over the item at (2, 0)
     * and add a new cell at (2, 1).
     *
     * @throws IllegalArgumentException if the cell is being placed in a spot that's already taken.
     */
    fun cell(row: Int? = null, col: Int? = null, rowSpan: Int = 1, colSpan: Int = 1, justification: Justification? = null, render: CellRenderBlock = {}) {
        // If last cell is on (row 1, col 1) and user specified `cell(row = 2)`, we should start at col 0 on that new
        // row, not whatever the random next column would be.
        @Suppress("NAME_SHADOWING") val col = col ?: if (row == null) nextEmptyCellCol else 0
        @Suppress("NAME_SHADOWING") val row = row ?: nextEmptyCellRow


        require(row >= 0) { "row must be non-negative" }
        require(col >= 0) { "col must be non-negative" }
        require(rowSpan > 0) { "rowSpan must be positive" }
        require(colSpan > 0) { "colSpan must be positive" }
        require(col + colSpan <= cols.specs.size) { "`colSpan` must not push `col` over the number of columns." }

        (cellIndex(row + rowSpan - 1, col + colSpan - 1)).let { botRightCellIndex ->
            while (botRightCellIndex >= _cellData.size) {
                _cellData.add(null)
            }
        }

        for (deltaX in 0 until colSpan) {
            for (deltaY in 0 until rowSpan) {
                val cellIndex = cellIndex(row + deltaY, col + deltaX)
                require(cellIndex > _cellData.lastIndex || _cellData[cellIndex] == null) {
                    "Attempting to declare a grid cell over a spot that's already taken: ($row, $col, rowSpan=$rowSpan, colSpan=$colSpan)"
                }
            }
        }

        cellIndex(row, col).let { topLeftCellIndex ->
            val topLeftData = CellData.Item(render, justification, width = colSpan, height = rowSpan)
            _cellData[topLeftCellIndex] = topLeftData

            for (deltaX in 0 until colSpan) {
                for (deltaY in 0 until rowSpan) {
                    if (deltaX != 0 || deltaY != 0) {
                        _cellData[cellIndex(row + deltaY, col + deltaX)] = CellData.Ptr(topLeftData, deltaX, deltaY)
                    }
                }
            }

            // Find the next empty cell
            var emptyCellIndex = topLeftCellIndex
            while (emptyCellIndex < _cellData.size && _cellData[emptyCellIndex] != null) {
                emptyCellIndex++
            }
            nextEmptyCellRow = emptyCellIndex / cols.specs.size
            nextEmptyCellCol = emptyCellIndex % cols.specs.size
        }
    }
}

object HorizontalSeparatorIndices {
    /** Include all row separators throughout the whole grid, the default aesthetic. */
    val All: Indices = indicesOf { addRange(0, -1) }

    /** Skip all row separators throughout the whole grid, for maximum compactness. */
    val None: Indices = Indices.Empty

    /** Skip all row separators throughout the whole grid EXCEPT the very first and last rows. */
    val TopAndBottom: Indices = indicesOf(0, -1)

    /**
     * Skip all row separators throughout the whole grid EXCEPT the top two and last rows.
     *
     * This is ideal if your grid has a header row that you'd like to be visually distinct from the rest of your grid.
     */
    val HeaderAndBottom: Indices = indicesOf(0, 1, -1)
}

/**
 * Declare a grid of cells.
 *
 * With grids, you define columns explicitly. Rows are added automatically as needed.
 *
 * Here, we create a grid of two 7-width columns:
 * ```
 * grid(Cols(7, 7), paddingLeftRight = 1) {
 *   cell { textLine("Cell 1a") }
 *   cell { textLine("Cell 1b") }
 *   cell { textLine("Cell 2a") }
 *   cell { textLine("Cell 2b") }
 * }
 * ```
 *
 * which renders:
 * ```
 * +---------+---------+
 * | Cell 1a | Cell 1b |
 * +---------+---------+
 * | Cell 2a | Cell 2b |
 * +---------+---------+
 * ```
 *
 * It's worth noting that the final width of the grid above is actually 21, not 14, because of the extra border
 * characters (3) and padding (4). When declaring column widths, you are describing the widths for the non-padded
 * contents of the cells *only*.
 *
 * By default, a newly declared cell will search from left-to-right then top-to-bottom for the next empty slot *after*
 * the last cell you declared. You can also specify the specific row and column you want a cell to apply to, especially
 * useful for skipping over cells you want to leave empty:
 * ```
 * grid(Cols(7, 7), paddingLeftRight = 1) {
 *   cell { textLine("Cell 1a") }
 *   cell(row = 1, col = 1) { textLine("Cell 2b") }
 * }
 * ```
 *
 * which renders:
 * ```
 * +---------+---------+
 * | Cell 1a |         |
 * +---------+---------+
 * |         | Cell 2b |
 * +---------+---------+
 * ```
 *
 * Note in the above example that the `row` and `col` parameters are 0-indexed.
 *
 * If the contents of a cell can't fit, it will insert newlines:
 * ```
 * grid(Cols(6, 3)) {
 *   cell { textLine("Hello grid!") }
 *   cell { textLine("Hi!") }
 * }
 * ```
 *
 * which renders:
 * ```
 * +------+---+
 * |Hello |Hi!|
 * |grid! |   |
 * +------+---+
 * ```
 *
 * Another thing demonstrated above is that the height of the whole row is determined by the tallest one. You can pass
 * in [maxCellHeight] to limit how tall this can get.
 *
 * There are also **fit-sized** columns (which calculate their size based on the longest element in the column) and
 * **star-sized** columns (which take up the remaining space after all other columns have been calculated). If you
 * declare one or more star-sized columns, you should also be sure to set the [targetWidth] parameter which is used in
 * calculating their widths.
 *
 * ```
 * // Fit, 4, * (targetWidth=15, padding=1)
 * // Fit here is 3, so * is 15 - 4 - 3 = 8
 * +-----+------+----------+
 * | 1   | 1234 | 12345678 |
 * +-----+------+----------+
 * | 23  |      |          |
 * +-----+------+----------+
 * | 456 |      |          |
 * +-----+------+----------+
 * ```
 *
 * For grids with only fixed-width columns, using the convenience constructor that takes widths directly (as we did in
 * the examples above) is recommended. Otherwise, users should leverage the builder constructor, which offers more
 * control and is the only way to declare star and fit columns.
 *
 * Using the builder constructor, the declaration for the above table is:
 * ```
 * grid(Cols { fit(); fixed(4); star() }, targetWidth = 15, paddingLeftRight = 1) {
 *   // ...
 * }
 * ```
 *
 * Finally, cells take optional `rowSpan` and `colSpan` parameters so the user can declare that they should cover
 * multiple spaces:
 * ```
 * grid(Cols(4, 4, 4)) {
 *   cell(rowSpan = 2, colSpan = 2) {
 *     textLine("Top"); textLine("left"); textLine("cell")
 *   }
 *   // Declare a third row for this example to demonstrate how
 *   // rowSpan in this case stops on the second row.
 *   cell(row = 2)
 * }
 * ```
 *
 * which renders:
 * ```
 * +----+----+----+
 * |Top      |    |
 * |left     +----+
 * |cell     |    |
 * +----+----+----+
 * |    |    |    |
 * +----+----+----+
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
 * @param justification The default justification to use for all cells. You can override this value on a case-by
 *   case basis by passing in a value to the `justification` parameter of `cell` call, or for the entire column by
 *   passing in a justification value when building the columns specification (e.g.
 *   `Cols { fit(justification = CENTER) }`)
 * @param maxCellHeight The maximum height to allow cells to grow to, which can happen if a cell contains many newlines
 *   or a "*" column gets squished a lot, forcing newlines to be inserted.
 * @param horizontalSeparatorIndices Which horizontal separators to include when rendering this grid. While rendering
 *   all is the default aesthetic, a user may want to remove many or all separators to maximize how much data can fit in
 *   limited vertical space. Note that the first index represents the very top of the grid and the last index represents
 *   the bottom. So if your table has 3 rows in it, then 0 is the top, 1 and 2 are the row separators, and 3 is the
 *   bottom. Note that several default [HorizontalSeparatorIndices] are provided for your convenience.
 */
fun RenderScope.grid(
    cols: Cols,
    targetWidth: Int? = null,
    characters: GridCharacters = section.session.defaults.gridStyle,
    paddingLeftRight: Int = 0,
    justification: Justification = Justification.LEFT,
    maxCellHeight: Int = Int.MAX_VALUE,
    horizontalSeparatorIndices: Indices = HorizontalSeparatorIndices.All,
    render: GridScope.() -> Unit
) {
    require(targetWidth == null || targetWidth > 0) { "targetWidth, if set, must be positive" }
    require(paddingLeftRight >= 0) { "paddingLeftRight must be non-negative" }
    require(maxCellHeight > 0) { "maxCellHeight must be positive" }

    val gridScope = GridScope(cols)
    gridScope.render()

    if (gridScope.cellData.isEmpty()) return

    addNewlinesIfNecessary(1)
    val colWidthsWithoutPadding = run {
        val colMinWidths = cols.specs.mapIndexed { x, spec ->
            when (spec) {
                is Cols.Spec.Fit -> {
                    var fitWidth = 1
                    var cellIndex = x
                    while (cellIndex < gridScope.cellData.size) {
                        val cellBlock =
                            (gridScope.cellData.getOrNull(cellIndex) as? CellData.Item)
                                ?.takeIf { it.width == 1 } // Don't include mutli-column cells in this calculation
                                ?.cellBlock
                        if (cellBlock != null) {
                            val cellRenderer = this.offscreen(Int.MAX_VALUE) { cellBlock(CellMetrics.Measuring) }
                            cellRenderer.lineWidths.maxOrNull()?.let { cellWidth ->
                                fitWidth = maxOf(fitWidth, cellWidth)
                            }
                        }
                        cellIndex += cols.specs.size
                    }

                    fitWidth.coerceIn(spec.minWidth ?: 1, spec.maxWidth ?: Int.MAX_VALUE)
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

    fun CellData?.isExtendedHorizontalSpan() = this is CellData.Ptr && this.offsetX > 0
    fun CellData?.isVerticalSpan() =
        (this is CellData.Item && this.height > 1) || (this is CellData.Ptr && this.target.height > 1)

    fun CellData?.isLastVerticalCell() = this is CellData.Ptr && this.offsetY == this.target.height - 1

    // A useful way to test if we're in a vertical (row) span that hasn't terminated yet. This is important for
    // figuring out what horizontal borders we should be using.
    fun CellData?.isNonTerminatedVerticalCell() = this.isVerticalSpan() && !this.isLastVerticalCell()

    val rowCount =
        gridScope.cellData.size / cols.specs.size + if (gridScope.cellData.size % cols.specs.size > 0) 1 else 0
    val lastRowIndex = rowCount - 1
    // rowCount + 1 -> top (1) + bottom (1) + inner separators (rowCount - 1)
    val horizontalSeparatorIndicesResolved = horizontalSeparatorIndices.resolve(rowCount + 1)

    // Render top
    if (horizontalSeparatorIndicesResolved.contains(0)) {
        text(characters.topLeft)
        colWidthsWithPadding.forEachIndexed { i, width ->
            if (i > 0) {
                // Only create an interaction border piece if we're starting a new grid cell
                text(
                    if (gridScope.cellData.getOrNull(i)
                            .isExtendedHorizontalSpan()
                    ) characters.horiz else characters.topCross
                )
            }
            text(characters.horiz.toString().repeat(width))
        }
        textLine(characters.topRight)
    }

    val cellBuffers: Map<CellData, OffscreenBuffer> = run {
        @Suppress("LocalVariableName") val _cellBuffers = mutableMapOf<CellData, OffscreenBuffer>()
        gridScope.cellData.forEachIndexed { i, data ->
            val x = i % cols.specs.size
            when (data) {
                is CellData.Item -> {
                    // `data.width - 1` represents intermediate vertical borders that we can use as rendering space
                    // instead, e.g. `|12|345|67` vs. |123456789|` In the same way, we can stomp over intermediate
                    // padding, e.g. `| 12 | 345 |` vs. `| 12345678 |`
                    val totalSpace = (0 until data.width).sumOf {
                        colWidthsWithoutPadding[x + it]
                    } + (data.width - 1) + ((data.width - 1) * paddingLeftRight * 2)
                    _cellBuffers[data] = this.offscreen(totalSpace) {
                        data.cellBlock(
                            this,
                            CellMetrics(width = totalSpace)
                        )
                    }
                }

                is CellData.Ptr -> _cellBuffers[data] = _cellBuffers.getValue(data.target)
                else -> Unit
            }
        }
        _cellBuffers
    }

    val cellRenderers: Map<CellData, OffscreenCommandRenderer> = run {
        @Suppress("LocalVariableName") val _cellRenderers = mutableMapOf<CellData, OffscreenCommandRenderer>()
        gridScope.cellData.forEach { data ->
            when (data) {
                is CellData.Item -> _cellRenderers[data] = cellBuffers.getValue(data).createRenderer()
                is CellData.Ptr -> _cellRenderers[data] = _cellRenderers.getValue(data.target)
                else -> Unit
            }
        }
        _cellRenderers
    }

    fun CellData?.hasMoreToRender() = if (this != null) cellRenderers[this]?.hasNextRow() == true else false

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

        // Render cell contents if we have any. Return true if this should be considered handled, false if the caller
        // should do the rendering themselves.
        fun CellData?.renderCellContents(leadingChar: Char, colIndex: Int): Boolean {
            if (this == null) return false
            if (this.isExtendedHorizontalSpan()) return true // This cell was already rendered by its initial cell

            text(leadingChar)
            val buffer = cellBuffers.getValue(this)
            val renderer = cellRenderers.getValue(this)

            val extraSpace = buffer.maxWidth - buffer.lineWidths.getOrElse(renderer.nextRowIndex) { 0 }
            val finalJustification =
                this.justification
                    ?: cols.specs[colIndex].justification
                    ?: justification
            repeat(paddingLeftRight) { text(" ") }
            when (finalJustification) {
                Justification.LEFT -> {
                    renderer.renderNextRow()
                    repeat(extraSpace) { text(" ") }
                }

                Justification.CENTER -> {
                    val leftSpace = extraSpace / 2
                    val rightSpace = extraSpace - leftSpace
                    repeat(leftSpace) { text(" ") }
                    renderer.renderNextRow()
                    repeat(rightSpace) { text(" ") }
                }

                Justification.RIGHT -> {
                    repeat(extraSpace) { text(" ") }
                    renderer.renderNextRow()
                }
            }
            repeat(paddingLeftRight) { text(" ") }

            return true
        }

        val cells =
            gridScope.cellData.subList(y * cols.specs.size, min((y + 1) * cols.specs.size, gridScope.cellData.size))

        if (cells.all { it == null }) {
            // This happens if users used `cell(row, col)` to completely skip one or more rows
            renderEmptyRow()
        } else {
            // Cells that span multiple rows do NOT contribute to the height of the current row (except for the very
            // last row). For example, a long vertically spanning cell looks like this:
            // ┌─────┬─────┐
            // │Test1│TestA│
            // │Test2├─────┤
            // │Test3│TestB│
            // │Test4├─────┤
            // │Test5│TestC│
            // │Test6│     │
            // │Test7│     │
            // └─────┴─────┘
            val heightContributingCells = cells.mapNotNull { if (it.isNonTerminatedVerticalCell()) null else it }

            var lineIndex = 0
            // Always render at least one line, even if all cells are empty
            while (lineIndex < maxCellHeight && (lineIndex == 0 || heightContributingCells.any { it.hasMoreToRender() })) {
                for (x in cols.specs.indices) {
                    val cellData = gridScope.cellData.getOrNull(gridScope.cellIndex(y, x))
                    if (!cellData.renderCellContents(characters.vert, x)) {
                        text(characters.vert)
                        text(" ".repeat(colWidthsWithPadding[x]))
                    }
                }
                textLine(characters.vert)
                lineIndex++
            }
        }

        // Here, we are rendering the lines between rows. Note that spanning rows will render as normal
        if (y < lastRowIndex && horizontalSeparatorIndicesResolved.contains(y + 1)) {
            colWidthsWithPadding.forEachIndexed { x, width ->
                fun fillWithDashes() {
                    text(characters.horiz.toString().repeat(width))
                }

                val currCellData = gridScope.cellData.getOrNull(gridScope.cellIndex(y, x))

                // For the next part, imagine we are rendering the row border between 0 and 1 (indicated by `->` in the
                // comment below). The most difficult part is choosing the appropriate intersection cross pieces. Here
                // are all the possible cases:
                //
                //    A. no span     B. col 0 span  C. col 1 span  D. both cols
                //    ┌─────┬─────┐  ┌─────┬─────┐  ┌─────┬─────┐  ┌─────┬─────┐
                //    │     │     │  │     │     │  │     │     │  │     │     │
                // -> ├─────┼─────┤  │     ├─────┤  ├─────┤     │  │     │     │
                //    │     │     │  │     │     │  │     │     │  │     │     │
                //    └─────┴─────┘  └─────┴─────┘  └─────┴─────┘  └─────┴─────┘
                //
                //    E. row 0 span  F. row 1 span  G. both rows
                //    ┌───────────┐  ┌─────┬─────┐  ┌───────────┐
                //    │           │  │     │     │  │           │
                // -> ├─────┬─────┤  ├─────┴─────┤  ├───────────┤
                //    │     │     │  │           │  │           │
                //    └─────┴─────┘  └───────────┘  └───────────┘

                val spanColCurr = currCellData.isNonTerminatedVerticalCell()
                val leadingChar = when {
                    x == 0 -> when {
                        spanColCurr -> characters.vert
                        else -> characters.leftCross
                    }

                    else -> {
                        val spanColLeft =
                            gridScope.cellData.getOrNull(gridScope.cellIndex(y, x - 1)).isNonTerminatedVerticalCell()
                        when {
                            !spanColLeft && spanColCurr -> characters.rightCross
                            spanColLeft && !spanColCurr -> characters.leftCross
                            spanColLeft && spanColCurr -> characters.vert
                            else -> {
                                val nextRow = y + 1
                                val spanRowAbove = currCellData.isExtendedHorizontalSpan()
                                val spanRowBelow =
                                    gridScope.cellData.getOrNull(gridScope.cellIndex(nextRow, x))
                                        .isExtendedHorizontalSpan()

                                when {
                                    spanRowAbove && spanRowBelow -> characters.horiz
                                    spanRowAbove -> characters.topCross
                                    spanRowBelow -> characters.botCross
                                    else -> characters.cross
                                }
                            }
                        }
                    }
                }

                if (!spanColCurr || !currCellData.renderCellContents(leadingChar, x)) {
                    text(leadingChar)
                    fillWithDashes()
                }
            }
            val lastCellData = gridScope.cellData.getOrNull(gridScope.cellIndex(y, cols.specs.lastIndex))
            val trailingChar = when {
                lastCellData.isNonTerminatedVerticalCell() -> characters.vert
                else -> characters.rightCross
            }
            textLine(trailingChar)
        }
    }

    // Render bottom
    if (horizontalSeparatorIndicesResolved.contains(rowCount)) {
        text(characters.botLeft)
        colWidthsWithPadding.forEachIndexed { i, width ->
            if (i > 0) {
                // Only create an interaction border piece if we're starting a new grid cell
                text(
                    if (gridScope.cellData.getOrNull(gridScope.cellIndex(lastRowIndex, i))
                            .isExtendedHorizontalSpan()
                    ) characters.horiz else characters.botCross
                )
            }
            text(characters.horiz.toString().repeat(width))
        }
        textLine(characters.botRight)
    }
}
