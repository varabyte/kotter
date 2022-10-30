import com.varabyte.kotter.foundation.anim.RenderAnim
import com.varabyte.kotter.foundation.anim.renderAnimOf
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.addTimer
import com.varabyte.kotterx.text.shiftRight
import java.time.Duration

private const val NUM_RAINBOW_COLORS = 90
private val RAINBOW_COLORS = (0 .. NUM_RAINBOW_COLORS).map { i ->
    // Use HSV instead of RGB because it's so much easier to loop through colors using it
    HSV(360 * (NUM_RAINBOW_COLORS - i) / NUM_RAINBOW_COLORS, 1.0f, 1.0f)
}

private const val NUM_FADE_OUT_COLORS = 50
private val FADE_OUT_COLORS = (0 .. NUM_FADE_OUT_COLORS).map { i ->
    val color = (255 * (NUM_FADE_OUT_COLORS - i)) / NUM_FADE_OUT_COLORS
    RGB(color, 0, 0)
}

fun main() = session {
    // Wait for a bit so there is some anticipation before the splash screen starts animating in.
    Thread.sleep(1000)

    // Thanks to https://patorjk.com/software/taag/#p=display&f=Larry%203D&t=Kotter for the text!
    val productNameLines =
        """
             __  __          __    __
            /\ \/\ \        /\ \__/\ \__
            \ \ \/'/'    ___\ \ ,_\ \ ,_\    __   _ __
             \ \ , <    / __`\ \ \/\ \ \/  /'__`\/\`'__\
              \ \ \\`\ /\ \L\ \ \ \_\ \ \_/\  __/\ \ \/
               \ \_\ \_\ \____/\ \__\\ \__\ \____\\ \_\
                \/_/\/_/\/___/  \/__/ \/__/\/____/ \/_/
        """.trimIndent().split("\n")

    val versionLines =
        """
               _          __
             /' \       /'__`\
            /\_, \     /\ \/\ \
            \/_/\ \    \ \ \ \ \
               \ \ \  __\ \ \_\ \
                \ \_\/\_\\ \____/
                 \/_/\/_/ \/___/
        """.trimIndent().split("\n")

    // 'length + 1' for num frames because we also include the empty string as a frame
    val wipeRightTextAnim = renderAnimOf(productNameLines.maxOf { it.length + 1 }, Duration.ofMillis(40), looping = false) { frameIndex ->
        for (y in productNameLines.indices) {
            textLine(productNameLines[y].take(frameIndex))
        }
    }
    val scrollUpTextAnim = renderAnimOf(versionLines.size, Duration.ofMillis(200), looping = false) { frameIndex ->
        for (i in 0 until (versionLines.size - frameIndex - 1)) {
            textLine()
        }
        for (i in 0 .. frameIndex) {
            textLine(versionLines[i])
        }
    }

    val rainbowAnim = renderAnimOf(RAINBOW_COLORS.size, Duration.ofMillis(10)) { i ->
        hsv(RAINBOW_COLORS[i])
    }
    val fadeOutAnim = renderAnimOf(FADE_OUT_COLORS.size, Duration.ofMillis(30), looping = false) { i ->
        rgb(FADE_OUT_COLORS[i])
    }

    var colorAnim by liveVarOf<RenderAnim?>(null)
    section {
        colorAnim?.invoke(this)

        textLine()
        // Splash text looks better if it's not hugging the left
        shiftRight(20) {
            wipeRightTextAnim(this)

            // The version string should appear at the bottom right
            shiftRight(23) {
                scrollUpTextAnim(this)
            }
        }
    }.runUntilSignal {
        addTimer(maxOf(scrollUpTextAnim.totalDuration, wipeRightTextAnim.totalDuration)) {
            colorAnim = rainbowAnim

            // Enjoy some rainbox colors looping for a little while
            addTimer(Duration.ofMillis(3000)) {
                colorAnim = fadeOutAnim

                addTimer(fadeOutAnim.totalDuration) {
                    signal()
                }
            }
        }
    }
}