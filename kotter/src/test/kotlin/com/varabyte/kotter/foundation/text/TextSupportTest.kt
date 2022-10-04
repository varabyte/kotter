package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.truthish.assertThat
import org.junit.Test

class TextSupportTest {
    @Test
    fun `trivial textLine calls in section`() = testSession { terminal ->
        section {
            textLine("Line 1")
            textLine("Line 2")
        }.run()

        assertThat(terminal.buffer).isEqualTo("Line 1\nLine 2\n${Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode()}")
    }

    @Test
    fun `trivial text calls in section`() = testSession { terminal ->
        section {
            text("Line 1")
            text("Line 2")
        }.run()

        // Even though the final command was just `text`, a newline is still inserted at the end.
        assertThat(terminal.buffer).isEqualTo("Line 1Line 2\n${Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode()}")
    }
}