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

class RenderAnimTest {
    @Test
    fun `simple render anim loops`() = testSession { terminal ->
        var timer: TestTimer? = null

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
    fun `can instantiate render anims via template`() = testSession { terminal ->
        var timer: TestTimer? = null

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
                    "> 1 <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 3 <",
                    "> 2 <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 1 <",
                    "> 3 <${Codes.Sgr.RESET}",
                    "",
                )
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> 2 <",
                    "> 1 <${Codes.Sgr.RESET}",
                    "",
                )
            }
        }
    }
}