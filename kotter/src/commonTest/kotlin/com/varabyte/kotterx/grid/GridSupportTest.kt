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
    fun `col size larger than width works`() = testSession { terminal ->
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
    fun `Cols fromStr can create column widths`() = testSession { terminal ->
        section {
            grid(cols = Cols.fromStr("3*, 4, 1*", 12)) {
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
    fun `justification works`() = testSession { terminal ->
        section {
            grid(Cols.uniform(3, 10), paddingLeftRight = 1, defaultJustification = Justification.CENTER) {
                cell(justification = Justification.LEFT) { textLine("Test") }
                cell { textLine("Test") }
                cell(justification = Justification.RIGHT) { textLine("Test") }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+----------+----------+----------+",
            "| Test     |   Test   |     Test |",
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
    fun `padding works`() = testSession { terminal ->
        section {
            grid(Cols(5, 5), paddingLeftRight = 2, paddingTopBottom = 1) {
                cell { textLine("X") }
                cell { textLine("YZ") }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "+-----+-----+",
            "|     |     |",
            "|  X  |  Y  |",
            "|     |  Z  |",
            "|     |     |",
            "+-----+-----+",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `padding with smaller col widths fails`() = testSession {
        section {
            assertThrows<IllegalArgumentException> {
                grid(Cols(8, 4), paddingLeftRight = 3) {
                    cell { textLine("A") }
                    cell { textLine("B") }
                }
            }.also { ex ->
                assertThat(ex.message!!).contains("4") // min width == 4
                assertThat(ex.message!!).contains("6") // total padding == 6
            }
        }.run()
    }

    @Test
    fun `invalid star widths fails`() = testSession {
        section {
            assertThrows<IllegalArgumentException> {
                grid(Cols.fromStr("*, 10")) {
                    cell { textLine("A") }
                    cell { textLine("B") }
                }
            }.also { ex ->
                assertThat(ex.message!!).contains("*")
            }
        }.run()
    }

    @Test
    fun `non-integer star widths fails`() = testSession {
        section {
            assertThrows<IllegalArgumentException> {
                grid(Cols.fromStr("1.5*")) {
                    cell { textLine("A") }
                    cell { textLine("B") }
                }
            }.also { ex ->
                assertThat(ex.message!!).contains("1.5*")
            }
        }.run()
    }
}
