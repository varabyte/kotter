import com.varabyte.kotter.foundation.anim.renderAnimOf
import com.varabyte.kotter.foundation.anim.text
import com.varabyte.kotter.foundation.anim.textAnimOf
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.p
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun main() = session {

    var result by liveVarOf<Int?>(null)
    val spinnerAnim = textAnimOf(listOf("\\", "|", "/", "-"), 125.milliseconds)

    // Same as: val thinkingAnim = textAnimOf(listOf("", ".", "..", "..."), Duration.ofMillis(500))
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