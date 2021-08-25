import com.varabyte.konsole.ansi.commands.text
import com.varabyte.konsole.core.runUntilSignal
import com.varabyte.konsole.konsole

fun main() {
    konsole {
        text("Please enter your first name: "); input()
    }.runUntilSignal {  }
}