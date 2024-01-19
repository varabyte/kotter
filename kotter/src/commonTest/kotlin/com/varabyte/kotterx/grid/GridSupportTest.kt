package com.varabyte.kotterx.grid

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class GridSupportTest {

    @Test
    fun `single cell grid works`() = testSession { terminal ->
        section {
            grid(4, 1) {
                cell {
                    textLine("Test")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Test",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `single cell grid of text works`() = testSession { terminal ->
        section {
            grid(4, 1) {
                cell {
                    text("Test")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Test",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `two cell grid with larger width than text works`() = testSession { terminal ->
        section {
            grid(6, 2) {
                cell {
                    textLine("Test")
                }
                cell {
                    textLine("Test2")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Test  Test2 ",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `two cell grid with multiple textLine works`() = testSession { terminal ->
        section {
            grid(6, 2) {
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
            "Test  Test  ",
            "Test  Test  ",
            "Test        ",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `two by two cell grid with multiple text works`() = testSession { terminal ->
        section {
            grid(6, 2) {
                cell {
                    text("Test")
                    text("12")
                }
                cell {
                    text("Test")
                    text("12")
                }

                cell {
                    text("Test")
                    text("12")
                }
                cell {
                    text("Test")
                    text("12")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Test12Test12",
            "Test12Test12",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `two by two cell grid with larger width than text works`() = testSession { terminal ->
        section {
            grid(6, 2) {
                cell {
                    textLine("Test")
                }
                cell {
                    textLine("Test2")
                }

                cell {
                    textLine("Test3")
                }
                cell {
                    textLine("Test4")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Test  Test2 ",
            "Test3 Test4 ",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `two by two cell grid with larger width than textLine works`() = testSession { terminal ->
        section {
            grid(6, 2) {
                cell {
                    textLine("Test")
                }
                cell {
                    textLine("Test2")
                }

                cell {
                    textLine("Test3")
                }
                cell {
                    textLine("Test4")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Test  Test2 ",
            "Test3 Test4 ",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `3 columns with 3 rows grid with empty cells works`() = testSession { terminal ->
        section {
            grid(6, 3) {
                cell {
                    textLine("Test")
                }
                cell {
                    textLine("Test2")
                }
                cell {

                }

                cell {
                    textLine("Test3")
                }
                cell {

                }
                cell {
                    textLine("Test4")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Test  Test2       ",
            "Test3       Test4 ",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `two by two cell grid with walls and padding and larger width than textLine works`() = testSession { terminal ->
        section {
            grid(6, 2, GridStyle(leftRightWalls = true, topBottomWalls = true, leftRightPadding = 1)) {
                cell {
                    textLine("Test")
                }
                cell {
                    textLine("Test2")
                }

                cell {
                    textLine("Test3")
                }
                cell {
                    textLine("Test4")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "------------------",
            "| Test   | Test2  |",
            "------------------",
            "| Test3  | Test4  |",
            "------------------",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `two by two cell grid with everything works`() = testSession { terminal ->
        section {
            grid(8, 2, GridStyle(leftRightWalls = true, topBottomWalls = true, leftRightPadding = 1)) {
                cell {
                    wrapTextLine("TestTestTestTest")
                }
                cell {
                    textLine("Test2")
                }

                cell {
                    textLine("Test3")
                }
                cell {
                    textLine("Test4")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "----------------------",
            "| TestTest | Test2    |",
            "| TestTest |          |",
            "----------------------",
            "| Test3    | Test4    |",
            "----------------------",
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }
}