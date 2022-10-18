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
    fun `can instantiate text anims via template`() = testSession { terminal ->
        var timer: TestTimer? = null
        // We have two anims running at the same time; wait for both of them to happen before allowing the section to
        // repaint, allowing us to test section state consistently
        val allowRenderToStart = ArrayBlockingQueue<Unit>(1).also {
            // Allow the first render to happen without getting blocked
            it.add(Unit)
        }
        val rendered = ArrayBlockingQueue<Unit>(1)

        val animTemplate = TextAnim.Template(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS)
        val anim1 = textAnimOf(animTemplate)
        val anim2 = textAnimOf(animTemplate)
        // Run tests at different times, to show they each own their own internal timer state
        var startRunningAnim2 = false
        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }

            text("> $anim1 <")
            if (startRunningAnim2) {
                textLine()
                text("~ $anim2 ~")
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
                "~ 1 ~${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 3 <",
                "~ 2 ~${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 1 <",
                "~ 3 ~${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 2 <",
                "~ 1 ~${Codes.Sgr.RESET}",
                "",
            ).inOrder()
        }
    }

    @Test
    fun `two anims with different rates can run at the same time`() = testSession { terminal ->
        var timer: TestTimer? = null
        // We have two anims running at the same time; wait for both of them to happen before allowing the section to
        // repaint, allowing us to test section state consistently
        val allowRenderToStart = ArrayBlockingQueue<Unit>(1).also {
            // Allow the first render to happen without getting blocked
            it.add(Unit)
        }
        val rendered = ArrayBlockingQueue<Unit>(1)

        val anim1 = textAnimOf(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS)
        val anim2 = textAnimOf(listOf("a", "b"), Anim.ONE_FRAME_60FPS.multipliedBy(2L))
        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }
            text("> $anim1:$anim2 <")
        }.onPreRender {
            allowRenderToStart.take()
        }.onRendered {
            rendered.add(Unit)
        }.run {
            @Suppress("NAME_SHADOWING") val timer = timer!!

            rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 1:a <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 2:a <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 3:b <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 1:b <${Codes.Sgr.RESET}",
                "",
            ).inOrder()

            timer.fastForward(Anim.ONE_FRAME_60FPS); allowRenderToStart.add(Unit); rendered.take()
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> 2:a <${Codes.Sgr.RESET}",
                "",
            ).inOrder()
        }
    }
}