import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.invert
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.timer.addTimer
import java.time.Duration

fun main() = session {
    section {
        textLine("Press SPACE to quit")
        textLine()
    }.run()

    val BLINK_LEN = Duration.ofMillis(250)
    var blinkOn by liveVarOf(false)
    section {
        scopedState {
            if (blinkOn) invert()
            textLine("This line will blink until SPACE is pressed, but will always turn off at that point.")
        }
    }.onFinishing {
        blinkOn = false
    }.runUntilKeyPressed(Keys.SPACE) {
        addTimer(BLINK_LEN, repeat = true) { blinkOn = !blinkOn }
    }
}