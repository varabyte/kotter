import com.varabyte.konsole.core.input.input
import com.varabyte.konsole.core.input.onInputEntered
import com.varabyte.konsole.core.input.runUntilInputEntered
import com.varabyte.konsole.core.konsoleApp
import com.varabyte.konsole.core.text.p
import com.varabyte.konsole.core.text.text
import com.varabyte.konsole.core.text.textLine

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
        onInputEntered { wantsToLearn = "yes".startsWith(input.lowercase()) }
    }
}