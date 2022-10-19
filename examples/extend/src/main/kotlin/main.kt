import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*

fun main() = session {
    var inputText = ""
    section {
        p {
            textLine("Type in a line of text")
            text("> "); input(Completions("lorem ipsum dolor sit amet"))
        }
    }.runUntilInputEntered {
        onInputEntered {
            if (input.isBlank()) rejectInput() else { inputText = input.trim() }
        }
    }

    section {
        p {
            cyan { textLine("Press Q to quit and SPACE to change between different effects") }
        }
    }.run()

    section {
        text("Your input \""); textorize(inputText); text("\" has been "); bold { text("textorized") }
        text(" (effect = ${getTextorizeEffect().toString().lowercase()})")
    }.runUntilKeyPressed(Keys.Q) {
        onKeyPressed {
            when (key) {
                Keys.SPACE -> setNextTextorizeEffect()
            }
        }
    }
}