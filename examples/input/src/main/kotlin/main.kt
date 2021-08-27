import com.varabyte.konsole.ansi.commands.text
import com.varabyte.konsole.ansi.commands.textLine
import com.varabyte.konsole.core.input.input
import com.varabyte.konsole.core.input.onInputChanged
import com.varabyte.konsole.core.input.onInputEntered
import com.varabyte.konsole.core.input.runUntilTextEntered
import com.varabyte.konsole.core.konsoleApp

fun main() = konsoleApp {
    var firstName by KonsoleVar("")
    var lastName by KonsoleVar("")

    konsole {
        text("Please enter your first name: "); input()
    }.runUntilTextEntered {
        onInputChanged { input = input.filter { it.isLetter() }.capitalize() }
        onInputEntered { firstName = input }
    }

    konsole {
        text("Please enter your last name: "); input()
    }.runUntilTextEntered {
        onInputChanged { input = input.filter { it.isLetter() }.capitalize() }
        onInputEntered { lastName = input }
    }

    konsole {
        textLine("Very nice to meet you, $firstName $lastName")
    }.run()
}