import com.varabyte.konsole.foundation.session
import com.varabyte.konsole.foundation.liveVarOf
import com.varabyte.konsole.foundation.text.textLine
import kotlinx.coroutines.delay

// https://github.com/JakeWharton/mosaic/tree/trunk/samples/counter
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