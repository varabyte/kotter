import com.varabyte.konsole.konsole
import com.varabyte.konsole.terminal.swing.SwingTerminal
import com.varabyte.konsole.text.*
import com.varabyte.konsole.text.ColorLayer.BG
import kotlinx.coroutines.runBlocking

fun main() {
    konsole {
        p {
            green(BG) {
                text("WELCOME TO KONSOLE!")
            }
            newLine()
        }
    }.run()

    konsole {
        underline {
            textLine("Nested colors test")
        }
        textLine("No colors")
        white(BG) {
            black {
                textLine("Black on white")
                blue(BG) {
                    textLine("Black on blue")
                }
                textLine("Black on white again")
            }
            red {
                textLine("Red on white")
            }
        }
        textLine("No colors again")
        newLine()
    }.run()

    konsole {
        underline {
            textLine("clearColors test")
        }

        red()
        textLine("Red text")
        white(BG)
        textLine("Red on white")
        clearColors()
        textLine("Text back to normal")
    }.run()
}