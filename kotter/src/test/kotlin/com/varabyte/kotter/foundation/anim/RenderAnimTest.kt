package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.timer.TestTimer
import com.varabyte.kotter.foundation.timer.useTestTimer
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.terminal.resolveRerenders
import com.varabyte.truthish.assertThat
import org.junit.Test
import java.util.concurrent.ArrayBlockingQueue

class RenderAnimTest {
    @Test
    fun `simple render anim loops`() = testSession { terminal ->
        var timer: TestTimer? = null
        val rendered = ArrayBlockingQueue<Unit>(1)

        val anim = renderAnimOf(3, Anim.ONE_FRAME_60FPS) {
            val frameOneIndexed = it + 1
            text("> $frameOneIndexed <")
        }
        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }
            anim(this)
        }.onRendered {
            rendered.add(Unit)
        }.run {
            @Suppress("NAME_SHADOWING") val timer = timer!!

            rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 1 <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 2 <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 3 <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 1 <${Codes.Sgr.RESET}",
                "",
            ).inOrder()
        }
    }
}