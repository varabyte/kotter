package com.varabyte.kotter.runtime

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.terminal.lines
import com.varabyte.truthish.assertThat
import kotlinx.coroutines.channels.Channel
import org.junit.Test
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch

class SectionTests {
    @Test
    fun `section always ends with a reset code and newline`() = testSession { terminal ->
        assertThat(terminal.buffer).isEmpty()
        section {}.run()

        assertThat(terminal.buffer).isEqualTo("${Codes.Sgr.RESET}\n")
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
    }
}