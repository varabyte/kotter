import com.varabyte.konsole.ansi.commands.textLine
import com.varabyte.konsole.core.KonsoleVar
import com.varabyte.konsole.core.konsoleApp
import kotlinx.coroutines.delay

fun main() = konsoleApp {
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