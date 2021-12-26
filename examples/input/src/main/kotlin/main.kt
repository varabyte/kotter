import com.varabyte.kotter.foundation.input.Completions
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.*

fun main() = session {
    var wantsToLearn by liveVarOf(false)
    section {
        text("Would you like to learn "); cyan { text("Kotter") }; textLine("? (Y/n)")
        text("> "); input(Completions("yes", "no"))

        if (wantsToLearn) {
            yellow(isBright = true) { p { textLine("""\(^o^)/""") } }
        }
    }.runUntilInputEntered {
        onInputEntered { wantsToLearn = "yes".startsWith(input.lowercase()) }
    }
}