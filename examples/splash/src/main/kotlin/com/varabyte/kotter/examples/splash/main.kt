package com.varabyte.kotter.examples.splash

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.anim.*
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotterx.text.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val NUM_RAINBOW_COLORS = 90
private val RAINBOW_COLORS = (0..NUM_RAINBOW_COLORS).map { i ->
    // Use HSV instead of RGB because it's so much easier to loop through colors using it
    HSV(360 * (NUM_RAINBOW_COLORS - i) / NUM_RAINBOW_COLORS, 1.0f, 1.0f)
}

private const val NUM_FADE_OUT_COLORS = 50
private val FADE_OUT_COLORS = (0..NUM_FADE_OUT_COLORS).map { i ->
    val color = (255 * (NUM_FADE_OUT_COLORS - i)) / NUM_FADE_OUT_COLORS
    // Start with yellow, as it's bright and has some impact for the final frame
    RGB(color, color, 0)
}

fun main() = session {
    // Wait for a bit so there is some anticipation before the splash screen starts animating in.
    Thread.sleep(1000)

    // Thanks to https://patorjk.com/software/taag/#p=display&f=Larry%203D&t=Kotter for the text!
    val titleLines =
        """
             __  __          __    __
            /\ \/\ \        /\ \__/\ \__
            \ \ \/'/'    ___\ \ ,_\ \ ,_\    __   _ __
             \ \ , <    / __`\ \ \/\ \ \/  /'__`\/\`'__\
              \ \ \\`\ /\ \L\ \ \ \_\ \ \_/\  __/\ \ \/
               \ \_\ \_\ \____/\ \__\\ \__\ \____\\ \_\
                \/_/\/_/\/___/  \/__/ \/__/\/____/ \/_/
        """.trimIndent().split("\n")

    // https://patorjk.com/software/taag/#p=display&f=Big&t=CLI+LIBRARY
    val subtitleLines =
        """
              _____ _      _____   _      _____ ____  _____            _______     __
             / ____| |    |_   _| | |    |_   _|  _ \|  __ \     /\   |  __ \ \   / /
            | |    | |      | |   | |      | | | |_) | |__) |   /  \  | |__) \ \_/ /
            | |    | |      | |   | |      | | |  _ <|  _  /   / /\ \ |  _  / \   /
            | |____| |____ _| |_  | |____ _| |_| |_) | | \ \  / ____ \| | \ \  | |
             \_____|______|_____| |______|_____|____/|_|  \_\/_/    \_\_|  \_\ |_|
        """.trimIndent().split("\n")

    // 'length + 1' for num frames because we also include the empty string as a frame
    val wipeRightTitleAnim =
        renderAnimOf(titleLines.maxOf { it.length + 1 }, 40.milliseconds, looping = false) { frameIndex ->
            for (y in titleLines.indices) {
                textLine(titleLines[y].take(frameIndex))
            }
        }
    val scrollUpSubtitleAnim = renderAnimOf(subtitleLines.size, 300.milliseconds, looping = false) { frameIndex ->
        for (i in 0 until (subtitleLines.size - frameIndex - 1)) {
            textLine()
        }
        for (i in 0..frameIndex) {
            textLine(subtitleLines[i])
        }
    }

    val rainbowAnim = renderAnimOf(RAINBOW_COLORS.size, 10.milliseconds) { i ->
        hsv(RAINBOW_COLORS[i])
    }
    val fadeOutAnim = renderAnimOf(FADE_OUT_COLORS.size, 30.milliseconds, looping = false) { i ->
        rgb(FADE_OUT_COLORS[i])
    }

    var colorAnim by liveVarOf<RenderAnim?>(null)

    val titleWidth = titleLines.maxOf { it.length }
    val subtitleWidth = subtitleLines.maxOf { it.length }
    var runSubtitleAnimation by liveVarOf(false)
    section {
        colorAnim?.invoke(this)

        // Splash text looks better if it's not hugging the left
        shiftRight((width - titleWidth).coerceAtLeast(0) / 2) {
            wipeRightTitleAnim(this)
        }
        if (runSubtitleAnimation) {
            textLine()
            shiftRight((width - subtitleWidth).coerceAtLeast(0) / 2) {
                scrollUpSubtitleAnim(this)
            }
        }
    }.runUntilSignal {
        // Show the title first, then the subtitle. And wait a beat after the title finishes showing; it feels better.
        addTimer(wipeRightTitleAnim.totalDuration + 1.seconds) {
            runSubtitleAnimation = true
            addTimer(scrollUpSubtitleAnim.totalDuration) {
                colorAnim = rainbowAnim

                // Enjoy some rainbow colors looping for a little while, then fade out
                addTimer(3.seconds) {
                    colorAnim = fadeOutAnim

                    addTimer(fadeOutAnim.totalDuration) {
                        signal()
                    }
                }
            }
        }
    }
}
