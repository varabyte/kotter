package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.timer.TestTimer
import com.varabyte.kotter.foundation.timer.useTestTimer
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.kotterx.test.runtime.blockUntilRenderWhen
import com.varabyte.kotterx.test.terminal.resolveRerenders
import kotlin.test.Test

class TextAnimTest {
    @Test
    fun `simple text anim loops`() = testSession { terminal ->
        var timer: TestTimer? = null

        val anim = textAnimOf(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS)
        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }
            text("> $anim <")
        }.run {
            @Suppress("NAME_SHADOWING") val timer = timer!!
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 1 <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 2 <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 3 <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 1 <${Codes.Sgr.RESET}",
                    "",
                )
            }
        }
    }

    @Test
    fun `anim timer paused while anim isn't rendered`() = testSession { terminal ->
        var timer: TestTimer? = null

        val anim = textAnimOf(listOf("1", "2", "3", "4", "5", "6", "7", "8"), Anim.ONE_FRAME_60FPS)
        var skipAnim = false
        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }
            if (!skipAnim) {
                text("> $anim <")
            }
        }.run {
            @Suppress("NAME_SHADOWING") val timer = timer!!
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 1 <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 2 <${Codes.Sgr.RESET}",
                    "",
                )
            }

            skipAnim = true
            // The timer update triggers a new repaint; however, the animation won't get hit this frame, meaning it
            // will pause its timer behind the scenes.
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "${Codes.Sgr.RESET}",
                    "",
                )
            }

            // Run a few more times, just to prove the point that when the animation restarts, it will be where it last
            // stopped, instead of at some frame in the future.
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            timer.fastForward(Anim.ONE_FRAME_60FPS)

            skipAnim = false
            rerender()
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 3 <${Codes.Sgr.RESET}",
                    "",
                )
            }
            // ^ We can tell the timer was reset because the animation went back to frame #3 instead of frame #6
        }
    }

    @Test
    fun `can instantiate text anims via template`() = testSession { terminal ->
        var timer: TestTimer? = null

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
        }.run {
            @Suppress("NAME_SHADOWING") val timer = timer!!

            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 1 <${Codes.Sgr.RESET}",
                    "",
                )
            }

            startRunningAnim2 = true
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 2 <",
                    "~ 1 ~${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 3 <",
                    "~ 2 ~${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 1 <",
                    "~ 3 ~${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 2 <",
                    "~ 1 ~${Codes.Sgr.RESET}",
                    "",
                )
            }
        }
    }

    @Test
    fun `two anims with different rates can run at the same time`() = testSession { terminal ->
        var timer: TestTimer? = null

        val anim1 = textAnimOf(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS)
        val anim2 = textAnimOf(listOf("a", "b"), Anim.ONE_FRAME_60FPS.multipliedBy(2L))
        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }
            text("> $anim1:$anim2 <")
        }.run {
            @Suppress("NAME_SHADOWING") val timer = timer!!

            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 1:a <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 2:a <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 3:b <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 1:b <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 2:a <${Codes.Sgr.RESET}",
                    "",
                )
            }

        }
    }
}