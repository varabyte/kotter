package com.varabyte.kotter.runtime

import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.render.aside
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.terminal.lines
import com.varabyte.kotter.terminal.resolveRerenders
import com.varabyte.kotter.terminal.typeChar
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import org.junit.Test
import java.util.concurrent.ArrayBlockingQueue
import kotlin.coroutines.cancellation.CancellationException

class SectionTests {
    @Test
    fun `section always ends with a reset code and newline`() = testSession { terminal ->
        assertThat(terminal.buffer).isEmpty()
        section {}.run()

        assertThat(terminal.buffer).isEqualTo("${Codes.Sgr.RESET}\n")
    }

    @Test
    fun `exceptions in run blocks are thrown`() = testSession {
        assertThrows<RuntimeException> {
            section {}.run {
                throw RuntimeException("Exception in run")
            }
        }.also {
            assertThat(it.message).isEqualTo("Exception in run")
        }
    }

    @Test
    fun `cancellations in run blocks are ignored`() = testSession {
        var runWasCalled = false
        section {}.run {
            runWasCalled = true
            throw CancellationException("CancellationException in run")
        }

        assertThat(runWasCalled).isTrue()
    }

    @Test
    fun `exceptions in section blocks are swallowed`() = testSession {
        var sectionWasCalled = false
        section {
            sectionWasCalled = true
            throw RuntimeException("Exception in section")
        }.run()

        assertThat(sectionWasCalled).isTrue()
    }

    @Test
    fun `multiple sections all append to the same buffer`() = testSession { terminal ->
        assertThat(terminal.buffer).isEmpty()
        section {}.run()
        section {}.run()
        section {}.run()

        assertThat(terminal.buffer).isEqualTo(
            "${Codes.Sgr.RESET}\n".repeat(3)
        )
    }

    @Test
    fun `single line sections get repainted in place`() = testSession { terminal ->
        var count by liveVarOf(0)

        val rendered = ArrayBlockingQueue<Unit>(1)
        section {
            text(count.toString())
        }.onRendered {
            rendered.add(Unit)
        }.run {
            rendered.take()
            count = 1
            rendered.take()
            count = 2
            rendered.take()
        }

        assertThat(terminal.lines()).containsExactly(
            "0${Codes.Sgr.RESET}"
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}1${Codes.Sgr.RESET}"
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}2${Codes.Sgr.RESET}",
            "", // Newline added at the end of the section
        ).inOrder()

        // Also, make sure the resolved view looks right
        assertThat(terminal.resolveRerenders()).containsExactly(
            "2${Codes.Sgr.RESET}",
            "", // Newline added at the end of the section
        ).inOrder()
    }

    @Test
    fun `multiline sections get repainted in place`() = testSession { terminal ->
        var count by liveVarOf(1)

        val rendered = ArrayBlockingQueue<Unit>(1)
        section {
            textLine("Multiple lines")
            text("Run #$count")
        }.onRendered {
            rendered.add(Unit)
        }.run {
            rendered.take()
            count++
            rendered.take()
            count++
            rendered.take()
        }

        assertThat(terminal.lines()).containsExactly(
            "Multiple lines",
            "Run #1${Codes.Sgr.RESET}"
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}${Codes.Cursor.MOVE_TO_PREV_LINE}\r${Codes.Erase.CURSOR_TO_LINE_END}"
                    + "Multiple lines",
            "Run #2${Codes.Sgr.RESET}"
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}${Codes.Cursor.MOVE_TO_PREV_LINE}\r${Codes.Erase.CURSOR_TO_LINE_END}"
                    + "Multiple lines",
            "Run #3${Codes.Sgr.RESET}",
            "", // Newline added at the end of the section
        ).inOrder()

        // Also, make sure the resolved view looks right
        assertThat(terminal.resolveRerenders()).containsExactly(
            "Multiple lines",
            "Run #3${Codes.Sgr.RESET}",
            "", // Newline added at the end of the section
        ).inOrder()
    }

    @Test
    fun `asides always finish running`() = testSession { terminal ->
        section {
            textLine()
            text("Section text")
        }.run {
            for (i in 1..5) {
                aside { text("Aside #$i") }
            }
        }

        // Thread timing can result in different intermediate buffer states. However, the final render, after all
        // rerenders are resolved, will be identical for all cases.
        assertThat(terminal.resolveRerenders()).containsExactly(
            "Aside #1${Codes.Sgr.RESET}",
            "Aside #2${Codes.Sgr.RESET}",
            "Aside #3${Codes.Sgr.RESET}",
            "Aside #4${Codes.Sgr.RESET}",
            "Aside #5${Codes.Sgr.RESET}",
            "",
            "Section text${Codes.Sgr.RESET}",
            "", // Newline added at the end of the section
        ).inOrder()
    }

    @Test
    fun `runUntilSignal exits after the signal is reached`() = testSession {
        section {}.runUntilSignal {
            signal()
        }
    }

    @Test
    fun `runUntilKeyPressed exits after the corresponding key is pressed`() = testSession { terminal ->
        section {}.runUntilKeyPressed(Keys.Q) {
            terminal.typeChar('q')
        }
    }
}