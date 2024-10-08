package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.terminal.inmemory.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class TextSupportTest {
    @Test
    fun `trivial textLine calls in section`() = testSession { terminal ->
        section {
            textLine("Line 1")
            textLine("Line 2")
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Line 1",
            "Line 2",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `trivial text calls in section`() = testSession { terminal ->
        section {
            text("Line 1")
            text("Line 2")
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Line 1Line 2${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `simple text effects work`() = testSession { terminal ->
        section {
            bold { red { textLine("Bold red") } }
            underline { green { textLine("Underline green") } }
            strikethrough { blue { textLine("Strikethrough blue") } }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            // Note: The order of the codes don't match the declaration above, because internally all states are
            // collected and applied in a somewhat arbitrary order that is implementation specific.
            "${Codes.Sgr.Colors.Fg.RED}${Codes.Sgr.Decorations.BOLD}Bold red",
            "${Codes.Sgr.Colors.Fg.GREEN}${Codes.Sgr.Decorations.UNDERLINE}${Codes.Sgr.Decorations.CLEAR_BOLD}Underline green",
            "${Codes.Sgr.Colors.Fg.BLUE}${Codes.Sgr.Decorations.CLEAR_UNDERLINE}${Codes.Sgr.Decorations.STRIKETHROUGH}Strikethrough blue",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }

    @Test
    fun `color arguments work`() = testSession { terminal ->
        section {
            yellow(ColorLayer.BG) {
                cyan(isBright = true) {
                    text("Text")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            // Note: The order of the codes don't match the declaration above, because internally all states are
            // collected and applied in a somewhat arbitrary order that is implementation specific.
            "${Codes.Sgr.Colors.Fg.CYAN_BRIGHT}${Codes.Sgr.Colors.Bg.YELLOW}Text${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `can specify color by index`() = testSession { terminal ->
        section {
            color(55) { text("Text") }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "${Codes.Sgr.Colors.Fg.lookup(55)}Text${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `can specify color by rgb and hsv`() = testSession { terminal ->
        section {
            rgb(255, 0, 255) { textLine("RGB1") }
            rgb(0xFFFF00) { textLine("RGB2") }
            hsv(300, 0.5f, 0.7f) { textLine("HSV") }
        }.run()

        val hsvToRgb = HSV(300, 0.5f, 0.7f).toRgb()
        assertThat(terminal.lines()).containsExactly(
            "${Codes.Sgr.Colors.Fg.truecolor(255, 0, 255)}RGB1",
            "${Codes.Sgr.Colors.Fg.truecolor(255, 255, 0)}RGB2",
            "${Codes.Sgr.Colors.Fg.truecolor(hsvToRgb.r, hsvToRgb.g, hsvToRgb.b)}HSV",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }

    @Test
    fun `can specify color by color enum`() = testSession { terminal ->
        section {
            Color.values().forEach { c ->
                color(c) { textLine(c.name) }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "${Codes.Sgr.Colors.Fg.BLACK}BLACK",
            "${Codes.Sgr.Colors.Fg.RED}RED",
            "${Codes.Sgr.Colors.Fg.GREEN}GREEN",
            "${Codes.Sgr.Colors.Fg.YELLOW}YELLOW",
            "${Codes.Sgr.Colors.Fg.BLUE}BLUE",
            "${Codes.Sgr.Colors.Fg.MAGENTA}MAGENTA",
            "${Codes.Sgr.Colors.Fg.CYAN}CYAN",
            "${Codes.Sgr.Colors.Fg.WHITE}WHITE",
            "${Codes.Sgr.Colors.Fg.BLACK_BRIGHT}BRIGHT_BLACK",
            "${Codes.Sgr.Colors.Fg.RED_BRIGHT}BRIGHT_RED",
            "${Codes.Sgr.Colors.Fg.GREEN_BRIGHT}BRIGHT_GREEN",
            "${Codes.Sgr.Colors.Fg.YELLOW_BRIGHT}BRIGHT_YELLOW",
            "${Codes.Sgr.Colors.Fg.BLUE_BRIGHT}BRIGHT_BLUE",
            "${Codes.Sgr.Colors.Fg.MAGENTA_BRIGHT}BRIGHT_MAGENTA",
            "${Codes.Sgr.Colors.Fg.CYAN_BRIGHT}BRIGHT_CYAN",
            "${Codes.Sgr.Colors.Fg.WHITE_BRIGHT}BRIGHT_WHITE",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }

    @Test
    fun `intermediate text effects are discarded`() = testSession { terminal ->
        section {
            bold {
                white {
                    black {
                        magenta {
                            clearBold()
                            text("Text")
                        }
                    }
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "${Codes.Sgr.Colors.Fg.MAGENTA}Text${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `can clear all effects`() = testSession { terminal ->
        section {
            bold {
                underline {
                    white {
                        black(layer = ColorLayer.BG) {
                            clearAll()
                            text("Text")
                        }
                    }
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Text${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `paragraph adds space around contents if necessary`() = testSession { terminal ->
        section {
            p {
                textLine("First paragraph")
            }

            p {
                textLine("Middle paragraph A")
            }

            p {
                text("Middle paragraph B")
            }

            p {
                text("Last paragraph")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "First paragraph", // No space above the first paragraph
            "",
            "Middle paragraph A", // No extra space added above when one paragraph follows a newline
            "",
            "Middle paragraph B",
            "",
            "Last paragraph",
            "",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }
}
