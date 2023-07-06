import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.text.*
import kotlinx.coroutines.delay

// Compare with: https://github.com/JakeWharton/mosaic/tree/trunk/samples/counter
fun main() = session {
    var count by liveVarOf(0)
    section {
        textLine("The count is: $count")
    }.run {
        for (i in 1..20) {
            delay(250)
            count++
        }
    }
}
