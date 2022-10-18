package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
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
            val frameNumber = it + 1
            text("> $frameNumber <")
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

    @Test
    fun `can instantiate render anims via template`() = testSession { terminal ->
        var timer: TestTimer? = null
        // We have two anims running at the same time; wait for both of them to happen before allowing the section to
        // repaint, allowing us to test section state consistently
        val allowRenderToStart = ArrayBlockingQueue<Unit>(1).also {
            // Allow the first render to happen without getting blocked
            it.add(Unit)
        }
        val rendered = ArrayBlockingQueue<Unit>(1)

        val animTemplate = RenderAnim.Template(3, Anim.ONE_FRAME_60FPS) {
            val frameNumber = it + 1
            text("> $frameNumber <")
        }
        val anim1 = renderAnimOf(animTemplate)
        val anim2 = renderAnimOf(animTemplate)
        // Run tests at different times, to show they each own their own internal timer state
        var startRunningAnim2 = false
        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }

            anim1(this)
            if (startRunningAnim2) {
                textLine()
                anim2(this)
            }
        }.onPreRender {
            allowRenderToStart.take()
        }.onRendered {
            rendered.add(Unit)
        }.run {
            @Suppress("NAME_SHADOWING") val timer = timer!!

            rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 1 <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            startRunningAnim2 = true
            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 2 <",
                "> 1 <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 3 <",
                "> 2 <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 1 <",
                "> 3 <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 2 <",
                "> 1 <${Codes.Sgr.RESET}",
                "",
            ).inOrder()
        }
    }
}