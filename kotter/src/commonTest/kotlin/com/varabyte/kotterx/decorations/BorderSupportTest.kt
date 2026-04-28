package com.varabyte.kotterx.decorations

import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.terminal.inmemory.lines
import com.varabyte.kotterx.test.foundation.testSession
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
            Codes.Sgr.Reset.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `border uses default style for session`() = testSession { terminal ->
        assertThat(defaults.borderStyle).isEqualTo(BorderCharacters.BoxThin)
        defaults.borderStyle = BorderCharacters.BoxDouble

        section {
            bordered {
                text("Test")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "╔════╗",
            "║Test║",
            "╚════╝",
            Codes.Sgr.Reset.toFullEscapeCode(),
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
            Codes.Sgr.Reset.toFullEscapeCode(),
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
            Codes.Sgr.Reset.toFullEscapeCode(),
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
            Codes.Sgr.Reset.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `can change border`() = testSession { terminal ->
        section {
            bordered(BorderCharacters.BoxDouble) {
                text("Test")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "╔════╗",
            "║Test║",
            "╚════╝",
            Codes.Sgr.Reset.toFullEscapeCode(),
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
            Codes.Sgr.Reset.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `works with double width characters and emoji`() = testSession { terminal ->
        section {
            bordered {
                textLine("X")
                // render width =
                //   "こんにちは" (10)
                //   "世界" (4)
                //   "(" (1)
                //   🌏 (2)
                // + ")" (1)
                // ---------------
                // 18
                textLine("こんにちは世界(\uD83C\uDF0F)")
                textLine("Y")
            }
        }.run()

        // NOTE: The following table may look weird in your IDE, but it should look perfect in a terminal!
        // This is because the font for asian characters probably isn't using monospace width.
        assertThat(terminal.lines()).containsExactly(
            "┌──────────────────┐",
            "│X                 │",
            "│こんにちは世界(\uD83C\uDF0F)│",
            "│Y                 │",
            "└──────────────────┘",
            Codes.Sgr.Reset.toFullEscapeCode(),
        ).inOrder()
    }

}
