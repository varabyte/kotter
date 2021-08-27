import com.varabyte.konsole.ansi.commands.p
import com.varabyte.konsole.ansi.commands.text
import com.varabyte.konsole.ansi.commands.textLine
import com.varabyte.konsole.core.input.input
import com.varabyte.konsole.core.input.onInputEntered
import com.varabyte.konsole.core.input.runUntilInputEntered
import com.varabyte.konsole.core.konsoleApp

fun main() = konsoleApp {
    var wantsToLearn by KonsoleVar(false)
    konsole {
        textLine("Would you like to learn Konsole? (Y/n)")
        text("> ")
        input()
        if (wantsToLearn) {
            p { textLine("""\(^o^)/""") }
        }
    }.runUntilInputEntered {
        onInputEntered { wantsToLearn = input.lowercase().startsWith("y") }
    }
}