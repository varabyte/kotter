package com.varabyte.kotter.runtime.terminal.inmemory

import com.varabyte.kotter.foundation.text.blue
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.terminal.TerminalSize
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class InMemoryTerminalTest {
    @Test
    fun `respects width and adds newlines as appropriate`() = testSession(TerminalSize.ofWidth(5)) { terminal ->
        section {
            // Add colors as misc ansi commands that shouldn't affect the width calculation
            red()
            textLine("1234") // width - 1
            green()
            textLine("12345") // width
            blue()
            textLine("123456") // width + 1
            white()
            textLine("1234567") // width + 2
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "${Codes.Sgr.Colors.Fg.Red}1234",
            "${Codes.Sgr.Colors.Fg.Green}12345",
            "${Codes.Sgr.Colors.Fg.Blue}12345",
            "6",
            "${Codes.Sgr.Colors.Fg.White}12345",
            "67",
            "${Codes.Sgr.Reset}"
        )
    }
}
