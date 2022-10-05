package com.varabyte.kotter.terminal

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.truthish.assertThat
import org.junit.Test

class TerminalTests {
    @Test
    fun `terminal always ends with a reset code and newline`() = testSession { terminal ->
        assertThat(terminal.buffer).isEmpty()
        section {}.run()

        assertThat(terminal.buffer).isEqualTo(Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode() + "\n")
    }
}