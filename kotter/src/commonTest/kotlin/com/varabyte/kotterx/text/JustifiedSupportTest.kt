package com.varabyte.kotterx.text

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.terminal.inmemory.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class JustifiedSupportTest {
    @Test
    fun `justified left text`() = testSession { terminal ->
        section {
            justified(Justification.LEFT) {
                textLine("12345678")
                textLine("1")
                textLine("12")
                textLine("123")
                textLine("1")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "12345678",
            "1       ",
            "12      ",
            "123     ",
            "1       ",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Run again, without right padding
        terminal.clear()

        section {
            justified(Justification.LEFT, padRight = false) {
                textLine("12345678")
                textLine("1")
                textLine("12")
                textLine("123")
                textLine("1")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "12345678",
            "1",
            "12",
            "123",
            "1",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `justified centered text`() = testSession { terminal ->
        section {
            justified(Justification.CENTER) {
                textLine("12345678")
                textLine("1")
                textLine("12")
                textLine("123")
                textLine("1")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "12345678",
            "   1    ",
            "   12   ",
            "  123   ",
            "   1    ",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Run again, without right padding
        terminal.clear()

        section {
            justified(Justification.CENTER, padRight = false) {
                textLine("12345678")
                textLine("1")
                textLine("12")
                textLine("123")
                textLine("1")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "12345678",
            "   1",
            "   12",
            "  123",
            "   1",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `justified right text`() = testSession { terminal ->
        section {
            justified(Justification.RIGHT) {
                textLine("12345678")
                textLine("1")
                textLine("12")
                textLine("123")
                textLine("1")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "12345678",
            "       1",
            "      12",
            "     123",
            "       1",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()

        // Run again, without right padding (which is meaningless for right justified text but whatever!)
        terminal.clear()

        section {
            justified(Justification.RIGHT, padRight = false) {
                textLine("12345678")
                textLine("1")
                textLine("12")
                textLine("123")
                textLine("1")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "12345678",
            "       1",
            "      12",
            "     123",
            "       1",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `minWidth can be set to affect the final result`() = testSession { terminal ->
        run { // Test left justified with minWidth > max line length
            terminal.clear()

            section {
                justified(Justification.LEFT, minWidth = 5) {
                    textLine("1")
                    textLine("12")
                    textLine("123")
                }
            }.run()

            assertThat(terminal.lines()).containsExactly(
                "1    ",
                "12   ",
                "123  ",
                Codes.Sgr.RESET.toFullEscapeCode(),
            ).inOrder()
        }

        run { // Test left justified with minWidth < max line length
            terminal.clear()

            section {
                justified(Justification.LEFT, minWidth = 2) {
                    textLine("1")
                    textLine("12")
                    textLine("123")
                }
            }.run()

            assertThat(terminal.lines()).containsExactly(
                "1  ",
                "12 ",
                "123",
                Codes.Sgr.RESET.toFullEscapeCode(),
            ).inOrder()
        }

        run { // Test center justified with minWidth > max line length
            terminal.clear()

            section {
                justified(Justification.CENTER, minWidth = 5) {
                    textLine("1")
                    textLine("12")
                    textLine("123")
                }
            }.run()

            assertThat(terminal.lines()).containsExactly(
                "  1  ",
                " 12  ",
                " 123 ",
                Codes.Sgr.RESET.toFullEscapeCode(),
            ).inOrder()
        }

        run { // Test center justified with minWidth < max line length
            terminal.clear()

            section {
                justified(Justification.CENTER, minWidth = 2) {
                    textLine("1")
                    textLine("12")
                    textLine("123")
                }
            }.run()

            assertThat(terminal.lines()).containsExactly(
                " 1 ",
                "12 ",
                "123",
                Codes.Sgr.RESET.toFullEscapeCode(),
            ).inOrder()
        }

        run { // Test right justified with minWidth > max line length
            terminal.clear()

            section {
                justified(Justification.RIGHT, minWidth = 5) {
                    textLine("1")
                    textLine("12")
                    textLine("123")
                }
            }.run()

            assertThat(terminal.lines()).containsExactly(
                "    1",
                "   12",
                "  123",
                Codes.Sgr.RESET.toFullEscapeCode(),
            ).inOrder()
        }

        run { // Test right justified with minWidth < max line length
            terminal.clear()

            section {
                justified(Justification.RIGHT, minWidth = 2) {
                    textLine("1")
                    textLine("12")
                    textLine("123")
                }
            }.run()

            assertThat(terminal.lines()).containsExactly(
                "  1",
                " 12",
                "123",
                Codes.Sgr.RESET.toFullEscapeCode(),
            ).inOrder()
        }
    }

}
