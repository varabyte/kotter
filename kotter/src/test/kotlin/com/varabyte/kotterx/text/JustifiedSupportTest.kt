package com.varabyte.kotterx.text

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.terminal.lines
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
}