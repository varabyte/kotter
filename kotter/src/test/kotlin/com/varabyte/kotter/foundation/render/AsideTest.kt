package com.varabyte.kotter.foundation.render

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.terminal.resolveRerenders
import com.varabyte.truthish.assertThat
import org.junit.Test

class AsideTest {
    @Test
    fun `asides auto add newlines at the end`() = testSession { terminal ->
        section {
            text("Section text")
        }.run {
            aside { text("Aside text") }
        }

        assertThat(terminal.resolveRerenders()).containsExactly(
            "Aside text${Ansi.Csi.Codes.Sgr.RESET}",
            "Section text${Ansi.Csi.Codes.Sgr.RESET}",
            "", // Newline added at the end of the section
        ).inOrder()

    }

    @Test
    fun `asides do not add newlines if already there`() = testSession { terminal ->
        section {
            text("Section text")
        }.run {
            aside { textLine("Aside text") }
        }

        assertThat(terminal.resolveRerenders()).containsExactly(
            "Aside text",
            "${Ansi.Csi.Codes.Sgr.RESET}Section text${Ansi.Csi.Codes.Sgr.RESET}",
            "", // Newline added at the end of the section
        ).inOrder()

    }
}