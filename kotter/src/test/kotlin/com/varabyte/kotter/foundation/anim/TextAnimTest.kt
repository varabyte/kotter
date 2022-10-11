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

class TextAnimTest {
    @Test
    fun `simple text anim loops`() = testSession { terminal ->
        var timer: TestTimer? = null
        val rendered = ArrayBlockingQueue<Unit>(1)

        val anim = textAnimOf(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS)
        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }
            text("> $anim <")
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

    @Test
    fun `two anims with different rates can run at the same time`() = testSession { terminal ->
        var timer: TestTimer? = null
        val rendered = ArrayBlockingQueue<Unit>(1)

        val anim1 = textAnimOf(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS)
        val anim2 = textAnimOf(listOf("a", "b"), Anim.ONE_FRAME_60FPS.multipliedBy(2L))
        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }
            text("> $anim1:$anim2 <")
        }.onRendered {
            rendered.add(Unit)
        }.run {
            @Suppress("NAME_SHADOWING") val timer = timer!!

            rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 1:a <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 2:a <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 3:b <${Codes.Sgr.RESET}",
                "",
            ).inOrder()
        }
    }
}