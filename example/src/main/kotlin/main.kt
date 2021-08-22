import com.varabyte.konsole.KonsoleSettings
import com.varabyte.konsole.konsole
import com.varabyte.konsole.terminal.swing.SwingTerminalIO
import com.varabyte.konsole.text.*
import com.varabyte.konsole.text.ColorLayer.BG

fun main() {
    // Default the example to ALWAYS using the virtual terminal. While perhaps not as nice as the system terminal, this
    // is guaranteed to work cross platform, and it is easier to debug as well (since IntelliJ / Gradle terminals seem
    // to fight with or don't support ANSI cursor commands.
    KonsoleSettings.provideTerminalIO = { SwingTerminalIO.create() }

//    run {
//        var count by KonsoleVar(0)
//        konsole {
//            textLine("*".repeat(count))
//        }.runUntilFinished {
//            while (count < 10) {
//                delay(250)
//                ++count
//            }
//        }
//    }

    konsole {
        textLine("WELCOME TO KONSOLE!")
        newLine()
    }.runOnce()

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
    }.runOnce()

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
    }.runOnce()
}