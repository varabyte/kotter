package com.varabyte.kotter.foundation.render

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.terminal.lines
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class OffscreenSupportTest {
    // Normally, you don't want to just render an offscreen buffer directly into a section (in that case, you wouldn't
    // need an offscreen buffer in the first place!). But, it's useful to do for tests.
    private fun OffscreenBuffer.renderDirectly() {
        val renderer = this.createRenderer()
        while (renderer.hasNextRow()) {
            renderer.renderNextRow()
            parentScope.textLine()
        }
    }

    private fun OffscreenBuffer.lines() = toRawText().split("\n")

    @Test
    fun `offscreen produces lines without newlines`() = testSession {
        lateinit var buffer: OffscreenBuffer
        section {
            buffer = offscreen {
                textLine("Line #1")
                textLine("Line #2")
                textLine("Line #3")
            }
        }.run()

        assertThat(buffer.lines()).containsExactly(
            "Line #1",
            "Line #2",
            "Line #3",
        ).inOrder()
    }

    @Test
    fun `offscreen buffer provides access to line lengths`() = testSession {
        lateinit var buffer: OffscreenBuffer
        section {
            buffer = offscreen {
                textLine("1")
                textLine("22")
                textLine("333")
                textLine("4444")
            }
        }.run()

        assertThat(buffer.lineLengths).containsExactly(1, 2, 3, 4).inOrder()
    }

    @Test
    fun `multipe renderers can be created at the same time`() = testSession { terminal ->
        section {
            val buffer = offscreen {
                textLine("Line #1")
                textLine("Line #2")
                textLine("Line #3")
            }

            val renderers = listOf(
                buffer.createRenderer(),
                buffer.createRenderer(),
                buffer.createRenderer(),
            )

            for (i in 0 until buffer.numLines) {
                renderers.forEach { r ->
                    r.renderNextRow()
                    textLine()
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Line #1",
            "Line #1",
            "Line #1",
            "Line #2",
            "Line #2",
            "Line #2",
            "Line #3",
            "Line #3",
            "Line #3",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `offscreen wont render unless asked`() = testSession { terminal ->
        section {
            textLine("Before offscreen")
            offscreen {
                textLine("Test")
            }
            textLine("After offscreen")
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Before offscreen",
            "After offscreen",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `offscreen wont render until asked`() = testSession { terminal ->
        section {
            val buffer = offscreen {
                textLine("Test")
            }

            textLine("Before offscreen")
            buffer.renderDirectly()
            textLine("After offscreen")
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Before offscreen",
            "Test",
            "After offscreen",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `offscreen call around text works`() = testSession { terminal ->
        section {
            textLine("Before offscreen")
            offscreen {
                text("Test")
            }.renderDirectly()
            textLine("After offscreen")
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Before offscreen",
            "Test", // renderDirectly adds a newline after the last line
            "After offscreen",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `offscreen call around textLine works`() = testSession { terminal ->
        section {
            textLine("Before offscreen")
            offscreen {
                textLine("Test")
            }.renderDirectly()
            textLine("After offscreen")
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Before offscreen",
            "Test",
            "After offscreen",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `offscreen sections maintain their own scoped state`() = testSession { terminal ->
        section {
            val buffer = offscreen {
                textLine("Inherited color (red)")
                cyan()
                textLine("Local color (cyan)")
                textLine("Still cyan")
            }

            val renderer = buffer.createRenderer()
            red()
            while (renderer.hasNextRow()) {
                text("red -- "); renderer.renderNextRow(); textLine(" -- red")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "${Codes.Sgr.Colors.Fg.RED}red -- Inherited color (red) -- red",
            "red -- ${Codes.Sgr.Colors.Fg.CYAN}Local color (cyan)${Codes.Sgr.Colors.Fg.RED} -- red",
            "red -- ${Codes.Sgr.Colors.Fg.CYAN}Still cyan${Codes.Sgr.Colors.Fg.RED} -- red",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

}