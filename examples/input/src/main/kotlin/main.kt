import com.varabyte.konsole.foundation.input.Completions
import com.varabyte.konsole.foundation.input.input
import com.varabyte.konsole.foundation.input.onInputEntered
import com.varabyte.konsole.foundation.input.runUntilInputEntered
import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.foundation.text.p
import com.varabyte.konsole.foundation.text.text
import com.varabyte.konsole.foundation.text.textLine

fun main() = konsoleApp {
    var wantsToLearn by konsoleVarOf(false)
    konsole {
        textLine("Would you like to learn Konsole? (Y/n)")
        text("> ")
        input(Completions("yes", "no"))
        if (wantsToLearn) {
            p { textLine("""\(^o^)/""") }
        }
    }.runUntilInputEntered {
        onInputEntered { wantsToLearn = "yes".startsWith(input.lowercase()) }
    }
}