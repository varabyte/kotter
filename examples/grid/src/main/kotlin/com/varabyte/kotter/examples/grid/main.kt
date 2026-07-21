package com.varabyte.kotter.examples.grid

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.terminal.onTerminalSizeChanged
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.terminal.EllipsisPresets
import com.varabyte.kotter.runtime.terminal.truncateToWidth
import com.varabyte.kotterx.grid.*
import com.varabyte.kotterx.text.*

// NOTE: The order these strategies are declared matters and controls how they are cycled through
private val HorizontalSeparatorStrategyNames = mapOf(
    HorizontalSeparatorIndices.All to "All",
    HorizontalSeparatorIndices.None to "None",
    HorizontalSeparatorIndices.TopAndBottom to "Top and bottom",
    HorizontalSeparatorIndices.HeaderAndBottom to "Header and bottom",
)

class GroceryItem(
    val name: String,
    val price: String,
    val notes: String? = null
)

fun main() = session {
    val minTableWidth = 10
    val reservedHorizontalSpace = 15 // Avoid filling up the whole screen and leave a bit of breathing space
    fun responsiveTableWidth() = (terminalSize.width - reservedHorizontalSpace).coerceAtLeast(minTableWidth)
    var tableWidth by liveVarOf(responsiveTableWidth())
    var responsive by liveVarOf(true)
    var usePadding by liveVarOf(true)
    var horizontalSeparatorStrategy by liveVarOf(HorizontalSeparatorIndices.All)

    val groceryItems = listOf(
        GroceryItem("bananas", "$0.59 / bunch", "Buy three bananas"),
        GroceryItem("celery", "$0.99"),
        GroceryItem("corn", "$3.00 / 4"),
        GroceryItem("avocados", "$2.00 / 3"),
        GroceryItem("ground beef", "$6.75 / lb", "Only buy 80/20; skip otherwise"),
    )

    section {
        fun pressKeyInfo(
            key: String,
            purpose: String,
            currValue: String? = null,
            isActive: Boolean = true,
            highlightKey: Boolean = true,
        ) {
            text("Press ")
            scopedState {
                if (highlightKey) cyan()
                text(key)
            }
            text(" to ")
            text(purpose)
            if (currValue != null) {
                text(": ")
                scopedState {
                    if (isActive) green() else red()
                    text(currValue)
                }
            }
            textLine()
        }
        fun pressKeyInfo(key: String, purpose: String, isOn: Boolean) {
            pressKeyInfo(key, purpose, if (isOn) "ON" else "OFF", isActive = isOn)
        }

        pressKeyInfo("SPACE", "stretch to screen width", responsive)
        scopedState {
            val enabled = tableWidth > minTableWidth
            if (!enabled) black(isBright = true)
            pressKeyInfo("LEFT", "shrink the table", highlightKey = enabled)
            pressKeyInfo("HOME", "minimize the table", highlightKey = enabled)
        }
        scopedState {
            val enabled = tableWidth < responsiveTableWidth()
            if (!enabled) black(isBright = true)
            pressKeyInfo("RIGHT", "grow the table", highlightKey = enabled)
            pressKeyInfo("END", "maximize the table", highlightKey = enabled)
        }

        pressKeyInfo("BACKSPACE", "toggle padding", usePadding)
        pressKeyInfo("H", "cycle horizontal separator types", HorizontalSeparatorStrategyNames.getValue(horizontalSeparatorStrategy))
        pressKeyInfo("Q", "quit")

        textLine()

        if (!responsive) {
            textLine("Target width: $tableWidth")
        } else {
            textLine("Responsive width: ${responsiveTableWidth()} (resize window to grow/shrink)")
        }

        val targetWidth = if (responsive) responsiveTableWidth() else tableWidth

        // We will show a multi-column layout for wide screens, or a 1-column layout otherwise
        val title = "Grocery List"
        if (targetWidth > 40) {
            grid(
                Cols { fit(); fit(); star(minWidth = 5) },
                targetWidth = targetWidth,
                characters = GridCharacters.Curved,
                paddingLeftRight = if (usePadding) 1 else 0,
                horizontalSeparatorIndices = horizontalSeparatorStrategy
            ) {
                cell(colSpan = 3, justification = Justification.CENTER) { bold(); text(title) }
                cell { bold(); text("Item") }
                cell { bold(); text("Price / lb.") }
                cell { bold(); text("Notes") }
                groceryItems.forEach { item ->
                    cell { text(item.name) }
                    cell { text(item.price) }
                    cell { cellMetrics ->
                        item.notes?.let {
                            text(
                                textMetrics.truncateToWidth(
                                    it,
                                    cellMetrics.width,
                                    ellipsis = EllipsisPresets.SYMBOL
                                )
                            )
                        }
                    }
                }
            }
        } else {
            grid(
                Cols { star(minWidth = title.length) },
                targetWidth = targetWidth,
                characters = GridCharacters.Curved,
                paddingLeftRight = if (usePadding) 1 else 0,
                horizontalSeparatorIndices = horizontalSeparatorStrategy
            ) {
                cell { bold(); text(title) }
                groceryItems.forEach { item ->
                    cell { cellMetrics ->
                        val firstLine = "${item.name} - ${item.price}"
                        textLine(
                            textMetrics.truncateToWidth(
                                firstLine,
                                cellMetrics.width,
                                ellipsis = EllipsisPresets.SYMBOL
                            )
                        )
                        item.notes?.let { notes ->
                            black(isBright = true)
                            textLine(
                                textMetrics.truncateToWidth(
                                    notes,
                                    cellMetrics.width,
                                    ellipsis = EllipsisPresets.SYMBOL
                                )
                            )
                        }
                    }
                }
            }
        }
    }.runUntilKeyPressed(Keys.Q) {
        onTerminalSizeChanged { if (responsive) rerender() }

        onKeyPressed {
            when (key) {
                Keys.Space -> {
                    responsive = !responsive
                    tableWidth = responsiveTableWidth()
                }
                Keys.Left -> {
                    responsive = false
                    if (tableWidth > minTableWidth) tableWidth--
                }
                Keys.Right -> {
                    responsive = false
                    if (tableWidth < responsiveTableWidth()) tableWidth++
                }
                Keys.Home -> {
                    responsive = false
                    tableWidth = minTableWidth
                }
                Keys.End -> {
                    responsive = false
                    tableWidth = responsiveTableWidth()
                }
                Keys.Backspace -> usePadding = !usePadding
                Keys.H -> {
                    val currStrategyIndex = HorizontalSeparatorStrategyNames.keys.indexOf(horizontalSeparatorStrategy)
                    val nextStrategyIndex = (currStrategyIndex + 1) % HorizontalSeparatorStrategyNames.size
                    horizontalSeparatorStrategy = HorizontalSeparatorStrategyNames.keys.toList()[nextStrategyIndex]
                }
            }
        }
    }
}
