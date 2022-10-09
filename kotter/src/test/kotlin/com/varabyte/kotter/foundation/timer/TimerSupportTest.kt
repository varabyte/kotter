package com.varabyte.kotter.foundation.timer

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.terminal.resolveRerenders
import com.varabyte.truthish.assertThat
import org.junit.Test
import java.time.Duration

class TimerSupportTest {
    @Test
    fun `timer is triggered once`() = testSession { terminal ->
        var count by liveVarOf(0)
        section {
            text(count.toString())
        }.run {
            val timer = data.useTestTimer()

            addTimer(Duration.ofMillis(5)) {
                ++count
            }

            timer.fastForward(Duration.ofMillis(5))
        }

        assertThat(terminal.resolveRerenders()).containsExactly(
            "1${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `timer is triggered repeatedly`() = testSession { terminal ->
        var count by liveVarOf(0)
        section {
            text(count.toString())
        }.run {
            val timer = data.useTestTimer()

            addTimer(Duration.ofMillis(5), repeat = true) {
                ++count
            }

            timer.fastForward(Duration.ofMillis(5))
            timer.fastForward(Duration.ofMillis(5))
            timer.fastForward(Duration.ofMillis(5))
        }

        assertThat(terminal.resolveRerenders()).containsExactly(
            "3${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `timers do not try to catch up if the elapsed time range is long`() = testSession { terminal ->
        var count by liveVarOf(0)
        section {
            text(count.toString())
        }.run {
            val timer = data.useTestTimer()

            addTimer(Duration.ofMillis(5), repeat = true) {
                ++count
            }

            // Only triggers a single timer update, not 10
            timer.fastForward(Duration.ofMillis(50))
        }

        assertThat(terminal.resolveRerenders()).containsExactly(
            "1${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }
}