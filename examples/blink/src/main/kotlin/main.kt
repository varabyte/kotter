import com.varabyte.konsole.foundation.input.Keys
import com.varabyte.konsole.foundation.input.runUntilKeyPressed
import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.foundation.text.invert
import com.varabyte.konsole.foundation.text.textLine
import com.varabyte.konsole.foundation.timer.addTimer
import java.time.Duration

fun main() = konsoleApp {
    konsole {
        textLine("Press SPACE to quit")
        textLine()
    }.run()

    val BLINK_LEN = Duration.ofMillis(250)
    var blinkOn by konsoleVarOf(false)
    konsole {
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