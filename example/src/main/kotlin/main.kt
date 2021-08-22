import com.varabyte.konsole.KonsoleSettings
import com.varabyte.konsole.core.scopedState
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
        bold { textLine("WELCOME TO KONSOLE!") }
        newLine()
    }.runOnce()

    konsole {
        underline {
            textLine("Decoration test")
        }

        bold { textLine("Bolded") }
        italic { textLine("Italicized") }
        strikethrough { textLine("Struck through") }

        scopedState {
            bold()
            italic()
            textLine("Bolded and italicized")
            underline {
                textLine("Bolded and italicized and underlined")
            }
            strikethrough {
                textLine("Bolded and italicized and struck through")
            }
        }

        textLine("Back to normal")
        newLine()
    }.runOnce()

    konsole {
        underline {
            textLine("Colors test")
        }

        // Verify nested colors work
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

        // Verify clearColors works
        red()
        textLine("Red text")
        white(BG)
        textLine("Red on white")
        clearColors()
        textLine("No colors again")
    }.runOnce()
}