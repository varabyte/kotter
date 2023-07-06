package com.varabyte.kotterx.decorations

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class BorderSupportTest {
    @Test
    fun `default bordered call around text works`() = testSession { terminal ->
        section {
            bordered {
                text("Test")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌────┐",
            "│Test│",
            "└────┘",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `default bordered call around textLine works`() = testSession { terminal ->
        section {
            bordered {
                textLine("Test")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌────┐",
            "│Test│",
            "└────┘",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `bordered supports empty content`() = testSession { terminal ->
        section {
            bordered {
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌┐",
            "││",
            "└┘",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }


    @Test
    fun `can handle multiple lines of different lengths`() = testSession { terminal ->
        section {
            bordered {
                textLine("Kinda long line")
                textLine("Short")
                textLine()
                textLine("Loooooooooooooooooooooong line")
                textLine("Short")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌──────────────────────────────┐",
            "│Kinda long line               │",
            "│Short                         │",
            "│                              │",
            "│Loooooooooooooooooooooong line│",
            "│Short                         │",
            "└──────────────────────────────┘",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `can change border`() = testSession { terminal ->
        section {
            bordered(BorderCharacters.BOX_DOUBLE) {
                text("Test")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "╔════╗",
            "║Test║",
            "╚════╝",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `can add padding`() = testSession { terminal ->
        section {
            bordered(paddingLeftRight = 2, paddingTopBottom = 1) {
                text("Test")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "┌────────┐",
            "│        │",
            "│  Test  │",
            "│        │",
            "└────────┘",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }
}
