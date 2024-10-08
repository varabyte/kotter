package com.varabyte.kotterx.test.terminal

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.terminal.inmemory.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.runtime.*
import kotlin.test.Test

class TerminalTestUtilsTest {
    @Test
    fun `assertMatches works`() = testSession { terminal ->
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
