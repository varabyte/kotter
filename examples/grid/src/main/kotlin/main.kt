import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotterx.grid.*
import com.varabyte.kotterx.text.*

private const val MIN_TABLE_WIDTH = 25
private const val MAX_TABLE_WIDTH = 40
private const val DEFAULT_TABLE_WIDTH = 30

fun main() = session {
    var tableWidth by liveVarOf(DEFAULT_TABLE_WIDTH)
    var usePadding by liveVarOf(true)

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
        textLine("Press Q to quit")
        textLine()

        textLine("Target width: $tableWidth")
        grid(
            Cols { fit(); fixed(10, Justification.CENTER); star(minWidth = 5)},
            targetWidth = tableWidth,
            characters = GridCharacters.CURVED,
            paddingLeftRight = if (usePadding) 1 else 0,
            maxCellHeight = 1
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
                Keys.LEFT -> if (tableWidth > MIN_TABLE_WIDTH) tableWidth--
                Keys.RIGHT -> if (tableWidth < MAX_TABLE_WIDTH) tableWidth++
                Keys.HOME -> tableWidth = MIN_TABLE_WIDTH
                Keys.END -> tableWidth = MAX_TABLE_WIDTH
                Keys.SPACE -> usePadding = !usePadding
            }
        }
    }
}
