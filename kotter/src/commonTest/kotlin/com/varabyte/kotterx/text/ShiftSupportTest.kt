package com.varabyte.kotterx.text

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class ShiftSupportTest {
    @Test
    fun `shift text to the right`() = testSession { terminal ->
        section {
            shiftRight(5) {
                textLine("1")
                textLine("23")
                textLine("456")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "     1",
            "     23",
            "     456",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }
}
