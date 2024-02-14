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
                    cell {}
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
            grid(cols = Cols.fromStr("3*, 4, 1*"), targetWidth = 12) {
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

            grid(cols = Cols.fromStr("2*, *, 3*"), paddingLeftRight = 1, targetWidth = 18) {
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
            grid(cols = Cols.fromStr("fit, fit, fit")) {
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
            grid(cols = Cols.fromStr("fit")) {
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
                Cols.fromStr("8, 8, 8 just:right"),
                paddingLeftRight = 1,
                defaultJustification = Justification.CENTER
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
    fun `padding works`() = testSession { terminal ->
        section {
            grid(Cols(1, 1), paddingLeftRight = 2, paddingTopBottom = 1) {
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
    fun `star widths without target width shrink to size 1`() = testSession { terminal ->
        section {
            grid(Cols.fromStr("*, 10*")) {
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
            grid(Cols.fromStr("1, 1, 1, 1"), maxCellHeight = 2) {
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
    fun `columns can set min and max widths`() = testSession { terminal ->
        section {
            grid(cols = Cols.fromStr("* min:5, fit max:5"), targetWidth = 1) {
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
