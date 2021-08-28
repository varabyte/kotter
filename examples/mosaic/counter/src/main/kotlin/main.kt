import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.foundation.text.textLine
import kotlinx.coroutines.delay

// https://github.com/JakeWharton/mosaic/tree/trunk/samples/counter
fun main() = konsoleApp {
    var count by konsoleVarOf(0)
    konsole {
        textLine("The count is: $count")
    }.run {
        for (i in 1..20) {
            delay(250)
            count++
        }
    }
}