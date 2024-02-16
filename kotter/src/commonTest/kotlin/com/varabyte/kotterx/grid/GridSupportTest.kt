package com.varabyte.kotterx.grid

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.kotterx.text.*
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlin.test.Test


class GridSupportTest {
    @Test
    fun `empty grids do not render`() = testSession { terminal ->
        section {
            grid(Cols(1, 2, 3)) {

            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
            "",
        ).inOrder()
    }

    @Test
    fun `blank cells still have height of 1`() = testSession { terminal ->
        section {
            grid(Cols(1, 2, 3)) {
                cell()
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+-+--+---+",
            "| |  |   |",
            "+-+--+---+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `invalid cell row and col values throw exceptions`() = testSession {
        section {
            assertThrows<IllegalArgumentException> {
                grid(Cols(1, 2, 3)) {
                    cell(row = -1) {  }
                }
            }
            assertThrows<IllegalArgumentException> {
                grid(Cols(1, 2, 3)) {
                    cell(col = -1) {  }
                }
            }
            assertThrows<IllegalArgumentException> {
                grid(Cols(1, 2, 3)) {
                    cell(col = 3) {  }
                }
            }
            assertThrows<IllegalArgumentException> {
                grid(Cols(1, 2, 3)) {
                    cell(rowSpan = 0) {  }
                }
            }
            assertThrows<IllegalArgumentException> {
                grid(Cols(1, 2, 3)) {
                    cell(colSpan = 0) {  }
                }
            }
            assertThrows<IllegalArgumentException> {
                grid(Cols(1, 2, 3)) {
                    cell(col = 1, colSpan = 3) {  }
                }
            }
        }.run()
    }

    @Test
    fun `col width larger than actual content`() = testSession { terminal ->
        section {
            grid(Cols(6)) {
                cell {
                    textLine("Test")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+------+",
            "|Test  |",
            "+------+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `col width must be greater than 0`() = testSession {
        section {
            assertThrows<IllegalArgumentException> {
                grid(Cols(0)) {
                    cell()
                }
            }
        }.run()
    }

    @Test
    fun `smaller widths break up cell contents`() = testSession { terminal ->
        section {
            grid(Cols(2, 5)) {
                cell {
                    textLine("Test")
                }
                cell {
                    textLine("Test 2")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+--+-----+",
            "|Te|Test |",
            "|st|2    |",
            "+--+-----+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `missing last cell gets filled in automatically`() = testSession { terminal ->
        section {
            grid(cols = Cols.uniform(2, width = 7)) {
                cell {
                    textLine("Test 11")
                }
                cell {
                    textLine("Test 12")
                }
                cell {
                    textLine("Test 21")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+-------+-------+",
            "|Test 11|Test 12|",
            "+-------+-------+",
            "|Test 21|       |",
            "+-------+-------+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `you can set cell row and col values explicitly`() = testSession { terminal ->
        section {
            grid(Cols(1, 1)) {
                cell { textLine("X") }
                cell { textLine("Y") }
                // Out of order
                cell(1, 1) { textLine("Z") }
                cell(1, 0) { textLine("A") }
                // Next cell automatically finds next slot after last filled in cell
                cell { textLine("B") }
                cell { textLine("C") }
                // You can skip multiple rows
                cell(5, 1) { textLine("!") }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+-+-+",
            "|X|Y|",
            "+-+-+",
            "|A|Z|",
            "+-+-+",
            "|B|C|",
            "+-+-+",
            "| | |",
            "+-+-+",
            "| | |",
            "+-+-+",
            "| |!|",
            "+-+-+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `can span content across rows or columns`() = testSession { terminal ->
        // NOTE: We use BOX_THIN characters for this test, so we can tell that the algorithm
        // is picking the right wall pieces

        // Span across cols
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(col = 1, colSpan = 2) {
                    textLine("Test")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───────┬───┐",
            "│   │ Test  │   │",
            "└───┴───────┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across rows
        terminal.clear()
        section {
            grid(Cols(1, 5, 1), characters = GridCharacters.BOX_THIN) {
                cell(col = 1, rowSpan = 2) {
                    textLine("Test1")
                    textLine("Test2")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌─┬─────┬─┐",
            "│ │Test1│ │",
            "├─┤Test2├─┤",
            "│ │     │ │",
            "└─┴─────┴─┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()


        // Span across full cols (top)
        terminal.clear()
        section {
            grid(Cols.uniform(3, width = 3), characters = GridCharacters.BOX_THIN) {
                cell(colSpan = 3)
                cell(row = 2)
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───────────┐",
            "│           │",
            "├───┬───┬───┤",
            "│   │   │   │",
            "├───┼───┼───┤",
            "│   │   │   │",
            "└───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across full cols (mid)
        terminal.clear()
        section {
            grid(Cols.uniform(3, width = 3), characters = GridCharacters.BOX_THIN) {
                cell(row = 1, colSpan = 3)
                cell(row = 2)
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┐",
            "│   │   │   │",
            "├───┴───┴───┤",
            "│           │",
            "├───┬───┬───┤",
            "│   │   │   │",
            "└───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across full cols (bottom)
        terminal.clear()
        section {
            grid(Cols.uniform(3, width = 3), characters = GridCharacters.BOX_THIN) {
                cell(row = 2, colSpan = 3)
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┐",
            "│   │   │   │",
            "├───┼───┼───┤",
            "│   │   │   │",
            "├───┴───┴───┤",
            "│           │",
            "└───────────┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across full rows (left)
        terminal.clear()
        section {
            grid(Cols.uniform(3, width = 3), characters = GridCharacters.BOX_THIN) {
                cell(rowSpan = 3)
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┐",
            "│   │   │   │",
            "│   ├───┼───┤",
            "│   │   │   │",
            "│   ├───┼───┤",
            "│   │   │   │",
            "└───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across full rows (mid)
        terminal.clear()
        section {
            grid(Cols.uniform(3, width = 3), characters = GridCharacters.BOX_THIN) {
                cell(col = 1, rowSpan = 3)
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┐",
            "│   │   │   │",
            "├───┤   ├───┤",
            "│   │   │   │",
            "├───┤   ├───┤",
            "│   │   │   │",
            "└───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across full rows (right)
        terminal.clear()
        section {
            grid(Cols.uniform(3, width = 3), characters = GridCharacters.BOX_THIN) {
                cell(col = 2, rowSpan = 3)
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┐",
            "│   │   │   │",
            "├───┼───┤   │",
            "│   │   │   │",
            "├───┼───┤   │",
            "│   │   │   │",
            "└───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Horizontal staircase
        terminal.clear()
        section {
            grid(Cols.uniform(4, width = 3), characters = GridCharacters.BOX_THIN) {
                cell(row = 1, col = 2, colSpan = 2)
                cell(row = 2, col = 1, colSpan = 3)
                cell(row = 3, col = 0, colSpan = 4)
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┬───┐",
            "│   │   │   │   │",
            "├───┼───┼───┴───┤",
            "│   │   │       │",
            "├───┼───┴───────┤",
            "│   │           │",
            "├───┴───────────┤",
            "│               │",
            "└───────────────┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Vertical staircase
        terminal.clear()
        section {
            grid(Cols.uniform(4, width = 3), characters = GridCharacters.BOX_THIN) {
                cell(row = 3)
                cell(row = 2, col = 1, rowSpan = 2)
                cell(row = 1, col = 2, rowSpan = 3)
                cell(row = 0, col = 3, rowSpan = 4)
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┬───┐",
            "│   │   │   │   │",
            "├───┼───┼───┤   │",
            "│   │   │   │   │",
            "├───┼───┤   │   │",
            "│   │   │   │   │",
            "├───┤   │   │   │",
            "│   │   │   │   │",
            "└───┴───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `can span content across rows and columns`() = testSession { terminal ->
        // Span across both rows and cols (top left)
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(0, 0, rowSpan = 2, colSpan = 2) {
                    textLine("Test")
                    textLine()
                    textLine("Test")
                }
                // Create an empty cell to force a final row
                cell(row = 3) { }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───────┬───┬───┐",
            "│ Test  │   │   │",
            "│       ├───┼───┤",
            "│ Test  │   │   │",
            "├───┬───┼───┼───┤",
            "│   │   │   │   │",
            "├───┼───┼───┼───┤",
            "│   │   │   │   │",
            "└───┴───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across both rows and cols (top)
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(0, 1, rowSpan = 2, colSpan = 2) {
                    textLine("Test")
                    textLine()
                    textLine("Test")
                }
                // Create an empty cell to force a final row
                cell(row = 3) { }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───────┬───┐",
            "│   │ Test  │   │",
            "├───┤       ├───┤",
            "│   │ Test  │   │",
            "├───┼───┬───┼───┤",
            "│   │   │   │   │",
            "├───┼───┼───┼───┤",
            "│   │   │   │   │",
            "└───┴───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across both rows and cols (top-right)
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(0, 2, rowSpan = 2, colSpan = 2) {
                    textLine("Test")
                    textLine()
                    textLine("Test")
                }
                // Create an empty cell to force a final row
                cell(row = 3) { }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───────┐",
            "│   │   │ Test  │",
            "├───┼───┤       │",
            "│   │   │ Test  │",
            "├───┼───┼───┬───┤",
            "│   │   │   │   │",
            "├───┼───┼───┼───┤",
            "│   │   │   │   │",
            "└───┴───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across both rows and cols (left)
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(1, 0, rowSpan = 2, colSpan = 2) {
                    textLine("Test")
                    textLine()
                    textLine("Test")
                }
                // Create an empty cell to force a final row
                cell(row = 3) { }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┬───┐",
            "│   │   │   │   │",
            "├───┴───┼───┼───┤",
            "│ Test  │   │   │",
            "│       ├───┼───┤",
            "│ Test  │   │   │",
            "├───┬───┼───┼───┤",
            "│   │   │   │   │",
            "└───┴───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across both rows and cols (center)
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(1, 1, rowSpan = 2, colSpan = 2) {
                    textLine("Test")
                    textLine()
                    textLine("Test")
                }
                // Create an empty cell to force a final row
                cell(row = 3) { }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┬───┐",
            "│   │   │   │   │",
            "├───┼───┴───┼───┤",
            "│   │ Test  │   │",
            "├───┤       ├───┤",
            "│   │ Test  │   │",
            "├───┼───┬───┼───┤",
            "│   │   │   │   │",
            "└───┴───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across both rows and cols (right)
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(1, 2, rowSpan = 2, colSpan = 2) {
                    textLine("Test")
                    textLine()
                    textLine("Test")
                }
                // Create an empty cell to force a final row
                cell(row = 3) { }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┬───┐",
            "│   │   │   │   │",
            "├───┼───┼───┴───┤",
            "│   │   │ Test  │",
            "├───┼───┤       │",
            "│   │   │ Test  │",
            "├───┼───┼───┬───┤",
            "│   │   │   │   │",
            "└───┴───┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across both rows and cols (bottom left)
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(2, 0, rowSpan = 2, colSpan = 2) {
                    textLine("Test")
                    textLine()
                    textLine("Test")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┬───┐",
            "│   │   │   │   │",
            "├───┼───┼───┼───┤",
            "│   │   │   │   │",
            "├───┴───┼───┼───┤",
            "│ Test  │   │   │",
            "│       ├───┼───┤",
            "│ Test  │   │   │",
            "└───────┴───┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across both rows and cols (bottom)
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(2, 1, rowSpan = 2, colSpan = 2) {
                    textLine("Test")
                    textLine()
                    textLine("Test")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┬───┐",
            "│   │   │   │   │",
            "├───┼───┼───┼───┤",
            "│   │   │   │   │",
            "├───┼───┴───┼───┤",
            "│   │ Test  │   │",
            "├───┤       ├───┤",
            "│   │ Test  │   │",
            "└───┴───────┴───┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Span across both rows and cols (bottom right)
        terminal.clear()
        section {
            grid(
                Cols.uniform(4, width = 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(2, 2, rowSpan = 2, colSpan = 2) {
                    textLine("Test")
                    textLine()
                    textLine("Test")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───┬───┬───┬───┐",
            "│   │   │   │   │",
            "├───┼───┼───┼───┤",
            "│   │   │   │   │",
            "├───┼───┼───┴───┤",
            "│   │   │ Test  │",
            "├───┼───┤       │",
            "│   │   │ Test  │",
            "└───┴───┴───────┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Can span across whole grid
        // NOTE: It's weird to set rowSpan when you don't even have multiple rows, but we should at least verify it
        // doesn't crash!
        terminal.clear()
        section {
            grid(
                Cols(1, 2, 3),
                characters = GridCharacters.BOX_THIN,
                justification = Justification.CENTER
            ) {
                cell(rowSpan = 2, colSpan = 3) {
                    textLine("Test")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌────────┐",
            "│  Test  │", // row 1
            "│        │", // row border (stomped over by rowspan)
            "│        │", // row 2
            "└────────┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `tall cell spanning multiple rows only pushes last row cells down`() = testSession { terminal ->
        section {
            grid(
                Cols.uniform(2, width = 5),
                characters = GridCharacters.BOX_THIN,
            ) {
                cell(row = 1, rowSpan = 2) {
                    for (i in 1..6) {
                        textLine("Test$i")
                    }
                }
                // Put some content in the right side just for comparison
                cell { textLine("TestA") }
                cell { textLine("TestB") }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌─────┬─────┐",
            "│     │     │",
            "├─────┼─────┤",
            "│Test1│TestA│",
            "│Test2├─────┤",
            "│Test3│TestB│",
            "│Test4│     │",
            "│Test5│     │",
            "│Test6│     │",
            "└─────┴─────┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `tall cell spanning multiple rows respects maxCellHeight`() = testSession { terminal ->
        section {
            grid(
                Cols(5, 6),
                characters = GridCharacters.BOX_THIN,
                maxCellHeight = 2
            ) {
                cell(rowSpan = 2) {
                    for (i in 1..9) {
                        textLine("Test$i")
                    }
                }
                // Put some content in the right side just for comparison
                cell { textLine("TestA1"); textLine("TestA2"); textLine("TestA3") }
                cell { textLine("TestB1"); textLine("TestB2"); textLine("TestB3") }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌─────┬──────┐",
            "│Test1│TestA1│",
            "│Test2│TestA2│",
            "│Test3├──────┤",
            "│Test4│TestB1│",
            "│Test5│TestB2│",
            "└─────┴──────┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `can change grid border characters`() = testSession { terminal ->
        section {
            grid(characters = GridCharacters.BOX_THIN, cols = Cols.uniform(2, width = 7)) {
                cell {
                    textLine("Test 11")
                }
                cell {
                    textLine("Test 12")
                }
                cell {
                    textLine("Test 21")
                }
                cell {
                    textLine("Test 22")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌───────┬───────┐",
            "│Test 11│Test 12│",
            "├───────┼───────┤",
            "│Test 21│Test 22│",
            "└───────┴───────┘",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `Cols fromStr works with dynamic sizing`() = testSession { terminal ->
        section {
            grid(cols = Cols { star(3); fixed(4); star(1) }, targetWidth = 12) {
                cell {
                    textLine("A")
                }
                cell {
                    textLine("B")
                }
                cell {
                    textLine("C")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+------+----+--+",
            "|A     |B   |C |",
            "+------+----+--+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `Star sized target width calculations ignore padding`() = testSession { terminal ->
        section {
            // targetWidth 18
            // ... plus col specs (2*, *, 3*) -> calculated size (6, 3, 9)
            // ... plus padding of 1 on each side -> padded size (8, 5, 11)
            // ... plus border walls (4 of them)  -> final total width = 8 + 5 + 11 + 4 = 28

            grid(cols = Cols { star(2); star(); star(3) }, paddingLeftRight = 1, targetWidth = 18) {
                cell {
                    textLine("A")
                }
                cell {
                    textLine("B")
                }
                cell {
                    textLine("C")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+--------+-----+-----------+",
            "| A      | B   | C         |",
            "+--------+-----+-----------+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `fit sizing works`() = testSession { terminal ->
        section {
            grid(cols = Cols { fit(); fit(); fit() }) {
                cell {
                    textLine("A")
                }
                cell {
                    textLine("BB")
                }
                cell {
                    textLine("C")
                }

                cell {
                    textLine("DD")
                }
                cell {
                    textLine("E")
                }
                cell {
                    textLine("FFFF")
                }

                cell {
                    textLine("GGG")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+---+--+----+",
            "|A  |BB|C   |",
            "+---+--+----+",
            "|DD |E |FFFF|",
            "+---+--+----+",
            "|GGG|  |    |",
            "+---+--+----+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `fit sizing takes newlines into account`() = testSession { terminal ->
        section {
            grid(cols = Cols { fit() }) {
                cell {
                    textLine("X")
                }
                cell {
                    textLine("Hello")
                    textLine("World!")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+------+",
            "|X     |",
            "+------+",
            "|Hello |",
            "|World!|",
            "+------+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `justification works`() = testSession { terminal ->
        section {
            // Precedence:
            // - Specific cell overrides
            // - Col overrides
            // - Grid default

            grid(
                Cols { fixed(8); fixed(8); fixed(8, justification = Justification.RIGHT) },
                paddingLeftRight = 1,
                justification = Justification.CENTER
            ) {
                cell(justification = Justification.LEFT) { textLine("Test") }
                cell { textLine("Test") }
                cell { textLine("Test") }

                cell { textLine("Test") }
                cell(justification = Justification.LEFT) { textLine("Test") }
                cell { textLine("Test") }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+----------+----------+----------+",
            "| Test     |   Test   |     Test |",
            "+----------+----------+----------+",
            "|   Test   | Test     |     Test |",
            "+----------+----------+----------+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `two cell grid with multiple textLine works`() = testSession { terminal ->
        section {
            grid(Cols.uniform(2, 6)) {
                cell {
                    textLine("Test")
                    textLine("Test")
                    textLine("Test")
                }
                cell {
                    textLine("Test")
                    textLine("Test")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+------+------+",
            "|Test  |Test  |",
            "|Test  |Test  |",
            "|Test  |      |",
            "+------+------+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `paddingLeftRight works`() = testSession { terminal ->
        section {
            grid(Cols(1, 1), paddingLeftRight = 2) {
                cell { textLine("X") }
                cell { textLine("YZ") }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+-----+-----+",
            "|  X  |  Y  |",
            "|     |  Z  |",
            "+-----+-----+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `star widths without target width shrink to size 1`() = testSession { terminal ->
        section {
            grid(Cols { star(); star(10) }) {
                cell { textLine("AA") }
                cell { textLine("BB") }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+-+-+",
            "|A|B|",
            "|A|B|",
            "+-+-+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `maxCellHeight can be used to limit number of cell rows`() = testSession { terminal ->
        section {
            grid(Cols.uniform(4, width = 1), maxCellHeight = 2) {
                cell { textLine("A") }
                cell { textLine("BB") }
                cell { textLine("CCC") }
                cell { textLine("DDDD") }
                cell { textLine("EEEE") }
                cell { textLine("FFF") }
                cell { textLine("GG") }
                cell { textLine("H") }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+-+-+-+-+",
            "|A|B|C|D|",
            "| |B|C|D|",
            "+-+-+-+-+",
            "|E|F|G|H|",
            "|E|F|G| |",
            "+-+-+-+-+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `non-fixed columns can set min and max widths`() = testSession { terminal ->
        section {
            grid(cols = Cols { star(minWidth = 5); fit(maxWidth = 5) }, targetWidth = 1) {
                cell {
                    textLine("A")
                }
                cell {
                    textLine("123456")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+-----+-----+",
            "|A    |12345|",
            "|     |6    |",
            "+-----+-----+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }
}
