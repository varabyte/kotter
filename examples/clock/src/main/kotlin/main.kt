import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

fun main() = session {
    section {
        p {
            textLine("Press Q to quit")
            textLine("Press SPACE to toggle 12hr / 24hr")
        }
    }.run()

    var dateReady by liveVarOf(false)
    var month by liveVarOf("")
    var day by liveVarOf(0)
    var currHour by liveVarOf(0)
    var currMin by liveVarOf(0)
    var amPm by liveVarOf("")
    var tick by liveVarOf(true)
    var elapsedSecs by liveVarOf(0)
    section {
        if (!dateReady) return@section

        textLine("$month $day")
        text("${currHour.toString().padStart(2, '0')}:${currMin.toString().padStart(2, '0')}")
        if (amPm.isNotEmpty()) text(" $amPm")
        textLine()
        textLine()
        textLine("This program has been running for ${elapsedSecs}s")
    }.runUntilKeyPressed(Keys.Q) {
        var isFormat12Hr = true
        fun updateDate() {
            val now = LocalDateTime.now()
            currMin = now.minute
            currHour = now.hour
            if (isFormat12Hr) {
                amPm = "A.M."
                if (currHour >= 12) {
                    amPm = "P.M."
                    currHour -= 12
                }
            } else {
                amPm = ""
            }
            month = now.month.name
            day = now.dayOfMonth
        }
        updateDate()
        dateReady = true

        addTimer(1.seconds, repeat = true) {
            tick = !tick
            elapsedSecs++
        }
        // We can have multiple timers. Query this one less frequently to avoid "expensive" date time querying, it's OK
        // if we're off by 5-10 seconds.
        addTimer(10.seconds, repeat = true) {
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
