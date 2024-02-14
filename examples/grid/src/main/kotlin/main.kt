import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotterx.grid.*

private const val MIN_TABLE_WIDTH = 20
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
            Cols.fromStr("fit, 7 just:center, * min:5"),
            targetWidth = tableWidth,
            characters = GridCharacters.CURVED,
            paddingLeftRight = if (usePadding) 1 else 0,
            maxCellHeight = 1
        ) {
            cell { bold(); text("Item") }
            cell { bold(); text("Price / lb.") }
            cell { bold(); text("Notes") }
            cell { text("Butter") }
            cell { text("$4.50") }
            cell(2, 0) { text("Tomatoes") }
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
