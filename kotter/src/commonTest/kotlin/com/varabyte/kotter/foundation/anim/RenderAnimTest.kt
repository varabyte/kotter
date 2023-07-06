package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.runtime.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class RenderAnimTest {
    @Test
    fun `simple render anim loops`() = testSession { terminal ->
        var timer: TestTimer? = null

        val anim = renderAnimOf(3, Anim.ONE_FRAME_60FPS) {
            val frameNumber = it + 1
            text("> $frameNumber <")
        }
        assertThat(anim.totalDuration).isEqualTo(Anim.ONE_FRAME_60FPS.times(3))

        section {
            if (timer == null) {
                // Need to initialize a test timer BEFORE we reference $anim for the first time
                timer = data.useTestTimer()
            }
            anim(this)
        }.run {
            @Suppress("NAME_SHADOWING") val timer = timer!!

            blockUntilRenderMatches(terminal) {
                text("> 1 <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 2 <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 3 <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 1 <")
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

            blockUntilRenderMatches(terminal) {
                text("> 1 <")
            }

            startRunningAnim2 = true
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                textLine("> 2 <")
                text("> 1 <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                textLine("> 3 <")
                text("> 2 <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                textLine("> 1 <")
                text("> 3 <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                textLine("> 2 <")
                text("> 1 <")
            }
        }
    }
}
