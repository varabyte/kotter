import com.varabyte.konsole.core.input.Keys
import com.varabyte.konsole.core.input.onKeyPressed
import com.varabyte.konsole.core.input.runUntilKeyPressed
import com.varabyte.konsole.core.konsoleApp
import com.varabyte.konsole.core.text.p
import com.varabyte.konsole.core.text.text
import com.varabyte.konsole.core.text.textLine
import com.varabyte.konsole.core.timer.addTimer
import java.time.Duration
import java.time.LocalDateTime

fun main() = konsoleApp {
    konsole {
        p {
            textLine("Press Q to quit")
            textLine("Press SPACE to toggle 12hr / 24hr")
        }
    }.run()

    var dateReady by KonsoleVar(false)
    var month by KonsoleVar("")
    var day by KonsoleVar(0)
    var currHour by KonsoleVar(0)
    var currMin by KonsoleVar(0)
    var amPm by KonsoleVar("")
    var tick by KonsoleVar(true)
    var elapsedSecs by KonsoleVar(0)
    konsole {
        if (!dateReady) return@konsole

        textLine("$month $day")
        text("${currHour.toString().padStart(2, '0')}:${currMin.toString().padStart(2, '0')}")
        if (amPm.isNotEmpty()) text(" $amPm")
        textLine()
        textLine()
        textLine("This program has been running for ${elapsedSecs}s")
        textLine()
    }.runUntilKeyPressed(Keys.Q) {
        var isFormat12Hr = true
        fun updateDate() {
            val now = LocalDateTime.now()
            currMin = now.minute
            currHour = now.hour
            if (isFormat12Hr) {
                amPm = "A.M."
                if (currHour > 12) {
                    amPm = "P.M."
                    currHour -= 12
                }
            }
            else {
                amPm = ""
            }
            month = now.month.name
            day = now.dayOfMonth
        }
        updateDate()
        dateReady = true

        addTimer(Duration.ofSeconds(1), repeat = true) {
            tick = !tick
            elapsedSecs++
        }
        // We can have multiple timers. Query this one less frequently to avoid "expensive" date time querying, it's OK
        // if we're off by 5-10 seconds.
        addTimer(Duration.ofSeconds(10), repeat = true) {
            updateDate()
        }
        onKeyPressed {
            if (key == Keys.SPACE) {
                isFormat12Hr = !isFormat12Hr
                updateDate()
            }
        }
    }
}