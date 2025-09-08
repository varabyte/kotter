import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import kotlin.time.Duration.Companion.milliseconds

fun main() = session {
    section {
        textLine("Press SPACE to quit")
        textLine()
    }.run()

    val BLINK_LEN = 250.milliseconds
    var blinkOn by liveVarOf(false)
    section {
        scopedState {
            if (blinkOn) invert()
            textLine("This line will blink until SPACE is pressed, but will always turn off at that point.")
        }
    }.onFinishing {
        blinkOn = false
    }.runUntilKeyPressed(Keys.Space) {
        addTimer(BLINK_LEN, repeat = true) { blinkOn = !blinkOn }
    }
}
