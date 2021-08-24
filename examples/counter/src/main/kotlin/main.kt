import com.varabyte.konsole.ansi.commands.*
import com.varabyte.konsole.core.KonsoleVar
import com.varabyte.konsole.konsole
import kotlinx.coroutines.delay

fun main() {
    var count by KonsoleVar(0)
    konsole {
        textLine("The count is: $count")
    }.run {
        for (i in 1..20) {
            delay(250)
            count++
        }
    }
}