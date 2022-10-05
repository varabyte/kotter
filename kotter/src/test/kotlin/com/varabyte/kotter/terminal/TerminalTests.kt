package com.varabyte.kotter.terminal

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.truthish.assertThat
import org.junit.Test

class TerminalTests {
    @Test
    fun `terminal always ends with a newline and reset code`() = testSession { terminal ->
        assertThat(terminal.buffer).isEmpty()
        section {}.run()

        assertThat(terminal.buffer).isEqualTo("\n" + Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode())
    }
}