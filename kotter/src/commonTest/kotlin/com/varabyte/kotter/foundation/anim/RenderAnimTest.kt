package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.runtime.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class RenderAnimTest {
    @Test
    fun `simple render anim loops`() = testSession { terminal ->
        val anim = renderAnimOf(3, Anim.ONE_FRAME_60FPS) {
            val frameNumber = it + 1
            text("> $frameNumber <")
        }
        assertThat(anim.totalDuration).isEqualTo(Anim.ONE_FRAME_60FPS.times(3))

        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            anim(this)
        }.run {
            val timer = data.useTestTimer()
            testTimerReady = true

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
        val animTemplate = RenderAnim.Template(3, Anim.ONE_FRAME_60FPS) {
            val frameNumber = it + 1
            text("> $frameNumber <")
        }
        val anim1 = renderAnimOf(animTemplate)
        val anim2 = renderAnimOf(animTemplate)
        // Run tests at different times, to show they each own their own internal timer state
        var startRunningAnim2 = false
        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            anim1(this)
            if (startRunningAnim2) {
                textLine()
                anim2(this)
            }
        }.run {
            val timer = data.useTestTimer()
            testTimerReady = true

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
