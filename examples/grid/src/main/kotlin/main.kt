import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotterx.grid.*
import com.varabyte.kotterx.text.*

private const val MIN_TABLE_WIDTH = 25
private const val MAX_TABLE_WIDTH = 40
private const val DEFAULT_TABLE_WIDTH = 30

// NOTE: The order these strategies are declared matters and controls how they are cycled through
private val HorizontalSeparatorStrategyNames = mapOf(
    HorizontalSeparatorIndices.All to "All",
    HorizontalSeparatorIndices.None to "None",
    HorizontalSeparatorIndices.TopAndBottom to "Top and bottom",
    HorizontalSeparatorIndices.HeaderAndBottom to "Header and bottom",
)

fun main() = session {
    var tableWidth by liveVarOf(DEFAULT_TABLE_WIDTH)
    var usePadding by liveVarOf(true)
    var horizontalSeparatorStrategy by liveVarOf(HorizontalSeparatorIndices.All)

    section {
        scopedState {
            if (tableWidth == MIN_TABLE_WIDTH) color(Color.BRIGHT_BLACK)
            textLine("Press LEFT to shrink the table")
        }
        scopedState {
            if (tableWidth == MAX_TABLE_WIDTH) color(Color.BRIGHT_BLACK)
            textLine("Press RIGHT to grow the table")
        }
        textLine("Press SPACE to toggle padding (currently ${if (usePadding) "on" else "off"})")
        textLine("Press H to cycle horizontal separator types (currently \"${HorizontalSeparatorStrategyNames.getValue(horizontalSeparatorStrategy)}\")")
        textLine("Press Q to quit")
        textLine()

        textLine("Target width: $tableWidth")
        grid(
            Cols { fit(); fixed(10, Justification.CENTER); star(minWidth = 5) },
            targetWidth = tableWidth,
            characters = GridCharacters.CURVED,
            paddingLeftRight = if (usePadding) 1 else 0,
            maxCellHeight = 1,
            horizontalSeparatorIndices = horizontalSeparatorStrategy
        ) {
            cell(colSpan = 3, justification = Justification.CENTER) { bold(); text("Grocery List") }
            cell { bold(); text("Item") }
            cell { bold(); text("Price / lb.") }
            cell { bold(); text("Notes") }
            cell { text("Butter") }
            cell { text("$4.50") }
            cell(row = nextEmptyCellRow + 1) { text("Tomatoes") }
            cell { text("$0.99") }
            cell { text("Skip if overripe") }
            cell { text("Beef") }
            cell { text("$2.99") }
            cell { text("Angus, 80/20") }
        }
    }.runUntilKeyPressed(Keys.Q) {
        onKeyPressed {
            when (key) {
                Keys.Left -> if (tableWidth > MIN_TABLE_WIDTH) tableWidth--
                Keys.Right -> if (tableWidth < MAX_TABLE_WIDTH) tableWidth++
                Keys.Home -> tableWidth = MIN_TABLE_WIDTH
                Keys.End -> tableWidth = MAX_TABLE_WIDTH
                Keys.Space -> usePadding = !usePadding
                Keys.H -> {
                    val currStrategyIndex = HorizontalSeparatorStrategyNames.keys.indexOf(horizontalSeparatorStrategy)
                    val nextStrategyIndex = (currStrategyIndex + 1) % HorizontalSeparatorStrategyNames.size
                    horizontalSeparatorStrategy = HorizontalSeparatorStrategyNames.keys.toList()[nextStrategyIndex]
                }
            }
        }
    }
}
