package com.varabyte.kotterx.test.terminal

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.bold
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.kotterx.test.runtime.blockUntilRenderWhen
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class TestTerminalTest {
    @Test
    fun `matches works`() = testSession { terminal ->
        // Make sure we do some slightly tricky stuff here so that different terminal histories which resolve to the
        // same final output are still considered a match.
        var counter by liveVarOf(0)
        section {
            bold { red { textLine("> $counter <") } }
        }.run {
            counter++
            blockUntilRenderWhen { terminal.resolveRerenders().first().contains("> 1 <") }
            counter++
            blockUntilRenderWhen { terminal.resolveRerenders().first().contains("> 2 <") }
            counter++
        }

        terminal.assertMatches {
            bold { red { textLine("> 3 <") } }
        }
    }
}