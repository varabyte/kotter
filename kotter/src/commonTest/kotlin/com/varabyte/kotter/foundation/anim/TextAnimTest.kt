package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.runtime.*
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class TextAnimTest {
    @Test
    fun `simple text anim loops`() = testSession { terminal ->
        val anim = textAnimOf(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS)
        assertThat(anim.totalDuration).isEqualTo(Anim.ONE_FRAME_60FPS.times(3))
        assertThat(anim.isRunning).isTrue()

        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            text("> $anim <")
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
    fun `textAnimOf uses session default animation duration`() = testSession {
        assertThat(defaults.animDuration).isEqualTo(Anim.ONE_FRAME_60FPS)
        run {
            val anim = textAnimOf(listOf("1", "2", "3"))
            assertThat(anim.totalDuration).isEqualTo(Anim.ONE_FRAME_60FPS.times(3))
        }

        defaults.animDuration = 100.milliseconds
        run {
            val anim = textAnimOf(listOf("1", "2", "3"))
            assertThat(anim.totalDuration).isEqualTo(300.milliseconds)
        }
    }

    @Test
    fun `can create non-looping text anim`() = testSession { terminal ->
        val anim = textAnimOf(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS, looping = false)
        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            text("> $anim <")
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
            assertThat(anim.isRunning).isFalse()

            // Set the animation back to an earlier frame and it will start running again
            anim.currFrame = 0
            assertThat(anim.isRunning).isTrue()
            blockUntilRenderMatches(terminal) {
                text("> 1 <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 2 <")
            }
        }
    }

    @Test
    fun `anim timer paused while anim isn't rendered`() = testSession { terminal ->
        val anim = textAnimOf(listOf("1", "2", "3", "4", "5", "6", "7", "8"), Anim.ONE_FRAME_60FPS)
        var skipAnim = false
        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            if (!skipAnim) {
                text("> $anim <")
            }
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

            skipAnim = true
            // The timer update triggers a new repaint; however, the animation won't get hit this frame, meaning it
            // will pause its timer behind the scenes.
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {}

            // Run a few more times, just to prove the point that when the animation restarts, it will be where it last
            // stopped, instead of at some frame in the future.
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            timer.fastForward(Anim.ONE_FRAME_60FPS)

            skipAnim = false; rerender()
            blockUntilRenderMatches(terminal) {
                text("> 3 <")
            }
            // ^ We can tell the timer was reset because the animation went back to frame #3 instead of frame #6
        }
    }

    @Test
    fun `can pause anim`() = testSession { terminal ->
        val anim = textAnimOf(listOf("1", "2", "3", "4", "5", "6", "7", "8"), Anim.ONE_FRAME_60FPS)
        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            text("> $anim <")
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

            anim.paused = true

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            timer.fastForward(Anim.ONE_FRAME_60FPS)

            anim.paused = false
            blockUntilRenderMatches(terminal) {
                text("> 2 <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 3 <")
            }
            // ^ We can tell the timer was reset because the animation went back to frame #1 instead of frame #3
        }
    }

    @Test
    fun `can set anim frame directly`() = testSession { terminal ->
        val anim = textAnimOf(listOf("1", "2", "3", "4", "5", "6", "7", "8"), Anim.ONE_FRAME_60FPS)
        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            text("> $anim <")
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
            timer.fastForward(Anim.ONE_FRAME_60FPS)
            timer.fastForward(Anim.ONE_FRAME_60FPS)

            blockUntilRenderMatches(terminal) {
                text("> 5 <")
            }

            anim.currFrame = 1
            blockUntilRenderMatches(terminal) {
                text("> 2 <")
            }

            // Curr frame can be set even if the animation is paused
            anim.paused = true
            anim.currFrame = 6
            blockUntilRenderMatches(terminal) {
                text("> 7 <")
            }

            // Curr frame request must be in bounds
            assertThrows<IllegalArgumentException> {
                anim.currFrame = -1
            }
            assertThrows<IllegalArgumentException> {
                anim.currFrame = 9999
            }
            assertThat(anim.currFrame).isEqualTo(6)
        }
    }

    @Test
    fun `can instantiate text anims via template`() = testSession { terminal ->
        val animTemplate = TextAnim.Template(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS)
        val anim1 = textAnimOf(animTemplate)
        val anim2 = textAnimOf(animTemplate)
        // Run tests at different times, to show they each own their own internal timer state
        var startRunningAnim2 = false
        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            text("> $anim1 <")
            if (startRunningAnim2) {
                textLine()
                text("~ $anim2 ~")
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
                text("~ 1 ~")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                textLine("> 3 <")
                text("~ 2 ~")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                textLine("> 1 <")
                text("~ 3 ~")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                textLine("> 2 <")
                text("~ 1 ~")
            }
        }
    }

    @Test
    fun `two anims with different rates can run at the same time`() = testSession { terminal ->
        val anim1 = textAnimOf(listOf("1", "2", "3"), Anim.ONE_FRAME_60FPS)
        val anim2 = textAnimOf(listOf("a", "b"), Anim.ONE_FRAME_60FPS.times(2))
        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            text("> $anim1:$anim2 <")
        }.run {
            val timer = data.useTestTimer()
            testTimerReady = true

            blockUntilRenderMatches(terminal) {
                text("> 1:a <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 2:a <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 3:b <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 1:b <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 2:a <")
            }

        }
    }

    @Test
    fun `can use extension text methods for appending the animation directly`() = testSession { terminal ->
        val anim1 = textAnimOf(listOf("1", "2"), Anim.ONE_FRAME_60FPS)
        val anim2 = textAnimOf(listOf("a", "b"), Anim.ONE_FRAME_60FPS)
        var testTimerReady by liveVarOf(false)
        section {
            if (!testTimerReady) return@section
            text("> "); text(anim1); text(':'); text(anim2); text(" <")
        }.run {
            val timer = data.useTestTimer()
            testTimerReady = true

            blockUntilRenderMatches(terminal) {
                text("> 1:a <")
            }

            timer.fastForward(Anim.ONE_FRAME_60FPS)
            blockUntilRenderMatches(terminal) {
                text("> 2:b <")
            }
        }
    }
}
