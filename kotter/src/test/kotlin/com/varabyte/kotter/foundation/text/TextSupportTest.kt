package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.terminal.lines
import com.varabyte.truthish.assertThat
import org.junit.Test

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
            Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
            ""
        )
    }

    @Test
    fun `trivial text calls in section`() = testSession { terminal ->
        section {
            text("Line 1")
            text("Line 2")
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Line 1Line 2" + Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode(),
            "", // Newline always added at the end of a section
        )
    }
}