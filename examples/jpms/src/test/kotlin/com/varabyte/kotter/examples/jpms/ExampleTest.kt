package com.varabyte.kotter.examples.jpms

import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.kotterx.test.terminal.assertMatches
import kotlin.test.Test

// This test just makes sure we can access the kotterx test support library
class ExampleTest {
    @Test
    fun runInMemoryTerminal() = testSession { terminal ->
        section {
            textLine("test")
        }.run()

        terminal.assertMatches {
            textLine("test")
        }
    }
}