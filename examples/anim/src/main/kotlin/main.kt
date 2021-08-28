import com.varabyte.konsole.foundation.anim.konsoleAnimOf
import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.foundation.text.green
import com.varabyte.konsole.foundation.text.p
import com.varabyte.konsole.foundation.text.text
import com.varabyte.konsole.foundation.text.textLine
import kotlinx.coroutines.delay
import java.time.Duration

fun main() = konsoleApp {
    var result by konsoleVarOf<Int?>(null)
    val spinnerAnim = konsoleAnimOf(listOf("\\", "|", "/", "-"), Duration.ofMillis(125))
    val thinkingAnim = konsoleAnimOf(listOf(".", "..", "..."), Duration.ofMillis(500))

    konsole {
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