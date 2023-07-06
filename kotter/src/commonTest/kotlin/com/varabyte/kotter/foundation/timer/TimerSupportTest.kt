package com.varabyte.kotter.foundation.timer

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class TimerSupportTest {
    @Test
    fun `timer is triggered once`() = testSession { terminal ->
        var count by liveVarOf(0)
        section {
            text(count.toString())
        }.run {
            val timer = data.useTestTimer()

            addTimer(5.milliseconds) {
                ++count
            }

            timer.fastForward(5.milliseconds)
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

            addTimer(5.milliseconds, repeat = true) {
                ++count
            }

            timer.fastForward(5.milliseconds)
            timer.fastForward(5.milliseconds)
            timer.fastForward(5.milliseconds)
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

            addTimer(5.milliseconds, repeat = true) {
                ++count
            }

            // Only triggers a single timer update, not 10
            timer.fastForward(50.milliseconds)
        }

        assertThat(terminal.resolveRerenders()).containsExactly(
            "1${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }
}
