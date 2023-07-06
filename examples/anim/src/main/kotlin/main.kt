import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.anim.*
import com.varabyte.kotter.foundation.text.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun main() = session {

    var result by liveVarOf<Int?>(null)
    val spinnerAnim = textAnimOf(listOf("\\", "|", "/", "-"), 125.milliseconds)

    // Same as: val thinkingAnim = textAnimOf(listOf("", ".", "..", "..."), 500.milliseconds)
    val thinkingAnim = renderAnimOf(4, 500.milliseconds) { frameIndex ->
        text(".".repeat(frameIndex))
    }

    section {
        val stillCalculating = (result == null)
        if (stillCalculating) {
            text(spinnerAnim)
        } else {
            green { text("✓") }
        }
        text(" Calculating")
        if (stillCalculating) {
            thinkingAnim(this)
        } else {
            textLine("... Done!")
            p {
                textLine("The answer is: $result")
            }
        }
    }.run {
        delay(4.seconds)
        result = 42
    }
}
