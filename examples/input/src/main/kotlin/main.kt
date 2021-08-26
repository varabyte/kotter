import com.varabyte.konsole.ansi.commands.text
import com.varabyte.konsole.core.input.input
import com.varabyte.konsole.core.konsoleApp
import com.varabyte.konsole.core.runUntilSignal

fun main() = konsoleApp {
    konsole {
        text("Please enter your first name: "); input()
    }.runUntilSignal {  }
}