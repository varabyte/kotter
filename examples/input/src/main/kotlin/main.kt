import com.varabyte.konsole.foundation.input.Completions
import com.varabyte.konsole.foundation.input.input
import com.varabyte.konsole.foundation.input.onInputEntered
import com.varabyte.konsole.foundation.input.runUntilInputEntered
import com.varabyte.konsole.foundation.session
import com.varabyte.konsole.foundation.liveVarOf
import com.varabyte.konsole.foundation.text.*

fun main() = session {
    var wantsToLearn by liveVarOf(false)
    section {
        text("Would you like to learn "); cyan { text("Konsole") }; textLine("? (Y/n)")
        text("> "); input(Completions("yes", "no"))

        if (wantsToLearn) {
            yellow(isBright = true) { p { textLine("""\(^o^)/""") } }
        }
    }.runUntilInputEntered {
        onInputEntered { wantsToLearn = "yes".startsWith(input.lowercase()) }
    }
}