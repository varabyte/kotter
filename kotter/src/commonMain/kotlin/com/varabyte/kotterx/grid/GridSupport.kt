package com.varabyte.kotterx.grid

import com.varabyte.kotter.foundation.render.OffscreenCommandRenderer
import com.varabyte.kotter.foundation.render.offscreen
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.Section
import com.varabyte.kotter.runtime.concurrent.createKey
import com.varabyte.kotter.runtime.render.OffscreenRenderScope
import com.varabyte.kotter.runtime.render.RenderScope
import kotlin.math.min

/**
 * When a `grid` block is defined, it creates this to manage state of all the child cells and `wrapText*` functions.
 */
data class GridContext(
    // Width of cell allowable, used in calculating how to fill a cell when empty or if not completely filled.
    val width: Int = 20,

    // The number of cells in a row. Once this number of cells is reached, they are rendered together to form a single
    // "line" of output, as that is how Kotter renders to the screen.
    val columns: Int,

    // The index of the current cell within a row. Necessary for cells to know when a row is filled and when rendering
    // of the row should begin.
    var cellIndex: Int,

    // The style to apply to the grid.
    val gridStyle: GridStyle,

    // All the buffers and their line lengths that form a single row of cells. They must all be rendered "together"
    // so the offscreen buffers won't insert newlines until after each cell appends its text to the end of the previous
    // cell in the row.
    var previousBuffers: MutableList<Pair<List<Int>, OffscreenCommandRenderer>> = mutableListOf(),
)

/**
 * User definable attributes about the grid to control how the cell walls and padding between cells looks.
 */
data class GridStyle(

    // Whether walls to the left and right of a cell should be drawn.
    val leftRightWalls:Boolean = false,

    // Whether walls to above and below a cell should be drawn.
    val topBottomWalls:Boolean = false,

    // how many spaces to the left and right of a cell's contents to add
    val leftRightPadding: Int = 0,
)

const val CELL_WALL = "|"
const val CELL_CEIL = "-"

val gridContextKey = Section.Lifecycle.createKey<GridContext>()

/**
 * We assume a `grid` parent was called. If not, this function will silently fail, as without a `grid` parent
 * there is no `GridContext` to define the behavior of a cell.
 */
fun RenderScope.cell(render: OffscreenRenderScope.() -> Unit) {
    val gridContext = data[gridContextKey] ?: return
    val cellPos = gridContext.cellIndex
    val columns = gridContext.columns
    val previousBuffers = gridContext.previousBuffers

    val content = offscreen(render)
    val renderer = content.createRenderer()
    previousBuffers.add(Pair(content.lineLengths, renderer))

    if (gridContext.cellIndex == columns - 1) {
        flushCells()
        gridContext.cellIndex = 0
    }
    else {
        gridContext.cellIndex = cellPos + 1
    }
}

/**
 * Once a row of cells is filled, the entire row is rendered all together in interleaving all the offscreen buffers
 * together into one line, one line at a time. This enables an arbitrary number of cells to append their text
 * one after another to the offscreen buffer before newlines are inserted. This is what allows the grid to
 *
 * We assume a `grid` parent was called. If not, this function will silently fail, as without a `grid` parent
 * there is no `GridContext` to define the behavior of a cell.
 */
fun RenderScope.flushCells() {
    val gridContext = data[gridContextKey] ?: return
    val previousBuffers = gridContext.previousBuffers
    val gridStyle = gridContext.gridStyle
    val width = gridContext.width
    val leftRightPadding = gridStyle.leftRightPadding
    var line = 0

    while (hasNextRows(previousBuffers)) {
        previousBuffers.forEach { buf ->
            if (gridStyle.leftRightWalls)
                text(CELL_WALL)
            val renderer = buf.second
            val lineLength = buf.first
            if (renderer.hasNextRow()) {
                repeat(leftRightPadding) { text(" ") }
                renderer.renderNextRow()
                repeat(width - lineLength[line] + leftRightPadding) { text(" ") }
            }
            else {
                repeat(leftRightPadding * 2 + width) { text(" ") }
            }
        }
        if (gridStyle.leftRightWalls)
            text(CELL_WALL)
        textLine()
        line++
    }

    renderTopBottomCellWalls()

    // clear the list so the next row of cells can fill with their buffers
    gridContext.previousBuffers = mutableListOf()
}

/**
 * Note: will do nothing if `grid` is not a parent.
 */
private fun RenderScope.renderTopBottomCellWalls() {
    val gridContext = data[gridContextKey] ?: return
    val columns = gridContext.columns
    val gridStyle = gridContext.gridStyle
    val width = gridContext.width
    val leftRightPadding = gridStyle.leftRightPadding

    if (gridStyle.topBottomWalls) {
        val wallExtra = if (gridStyle.leftRightWalls) 2 else 0
        repeat((leftRightPadding * 2 + width) * columns + wallExtra * (columns - 1)) { text(CELL_CEIL) }
        textLine()
    }
}

private fun hasNextRows(previousBuffers: MutableList<Pair<List<Int>, OffscreenCommandRenderer>>):
        Boolean = previousBuffers.fold(false) { anyHas, buf -> anyHas || buf.second.hasNextRow() }

fun RenderScope.grid(
    width: Int,
    columns: Int,
    gridStyle: GridStyle = GridStyle(),
    render: OffscreenRenderScope.() -> Unit) {
    data[gridContextKey] = GridContext(width, columns, 0, gridStyle)

    val content = offscreen(render)
    val renderer = content.createRenderer()
    renderTopBottomCellWalls()
    while (renderer.hasNextRow()) {
        renderer.renderNextRow()
        textLine()
    }

    // clean our cache to prevent leaks, and to prevent external `cell` instances from executing erroneously.
    data.remove(gridContextKey)
}

fun RenderScope.wrapText(text: String) {
    val width = data[gridContextKey]?.width ?: Int.MAX_VALUE
    if (text.length > width || text.lines().size > 1) {
        val chunks = text.length / width
        var index = 0
        while (index <= chunks) {

            // previous lines won't write a newline, so we only do it if we broke a line
            // and have more to process
            if (index > 0)
                textLine()
            val broken = text.substring(index * width, min((index + 1) * width, text.length)).trim()
            text(broken)
            index++
        }
    }
    else {
        text(text)
    }
}

fun RenderScope.wrapTextLine(text: String) {
    wrapText(text)
    textLine()
}
