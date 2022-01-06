import com.varabyte.kotter.foundation.anim.textAnimOf
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.p
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.coroutines.delay
import java.time.Duration

fun main() = session {
    var result by liveVarOf<Int?>(null)
    val spinnerAnim = textAnimOf(listOf("\\", "|", "/", "-"), Duration.ofMillis(125))
    val thinkingAnim = textAnimOf(listOf(".", "..", "..."), Duration.ofMillis(500))

    section {
        val stillCalculating = (result == null)
        if (stillCalculating) {
            text(spinnerAnim)
        } else {
            green { text("✓") }
        }
        text(" Calculating")
        if (stillCalculating) {
            text(thinkingAnim)
        } else {
            textLine("... Done!")
            p {
                textLine("The answer is: $result")
            }
        }
    }.run {
        delay(Duration.ofSeconds(4).toMillis())
        result = 42
    }
}