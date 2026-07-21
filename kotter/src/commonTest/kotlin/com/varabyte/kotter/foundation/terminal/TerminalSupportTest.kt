package com.varabyte.kotter.foundation.terminal

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.terminal.TerminalSize
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.kotterx.test.runtime.blockUntilRenderMatches
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class TerminalSupportTest {
    @Test
    fun `onTerminalSizeChanged fired when terminal size changes`() = testSession(TerminalSize.Default) { terminal ->
        fun shouldBeWideMode() = terminalSize.width >= 50
        assertThat(shouldBeWideMode()).isTrue() // TerminalSize.Default should be pretty wide!
        var isWideMode by liveVarOf(shouldBeWideMode())
        var renderCount = 0 // Intentionally NOT a live var. Used for testing render requests as a side-effect
        section {
            textLine("Is wide mode? $isWideMode")
            textLine("Render count: ${++renderCount}")
        }.run {
            onTerminalSizeChanged {
                isWideMode = shouldBeWideMode()
                assertThat(terminalSize).isEqualTo(section.session.terminalSize)
            }

            blockUntilRenderMatches(terminal) {
                textLine("Is wide mode? true")
                textLine("Render count: 1")
            }

            terminal.size = terminal.size.copy(width = 50)
            awaitActiveRender()

            assertThat(renderCount).isEqualTo(1) // Not rerendered!

            terminal.size = terminal.size.copy(width = 30)

            blockUntilRenderMatches(terminal) {
                textLine("Is wide mode? false")
                textLine("Render count: 2")
            }
        }
    }
}
