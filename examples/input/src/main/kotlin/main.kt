import com.varabyte.konsole.ansi.commands.textLine
import com.varabyte.konsole.core.runUntilSignal
import com.varabyte.konsole.konsole

fun main() {
    konsole {
        textLine("Please enter your first name: $input")
    }.runUntilSignal {  }
}