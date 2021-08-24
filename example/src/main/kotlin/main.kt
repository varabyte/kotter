import com.varabyte.konsole.KonsoleSettings
import com.varabyte.konsole.ansi.commands.*
import com.varabyte.konsole.ansi.commands.ColorLayer.BG
import com.varabyte.konsole.core.KonsoleVar
import com.varabyte.konsole.core.scopedState
import com.varabyte.konsole.konsole
import com.varabyte.konsole.terminal.swing.SwingTerminal
import kotlinx.coroutines.delay
import kotlin.random.Random

fun main() {
    konsole {
        bold { textLine("WELCOME TO KONSOLE!") }
        textLine()
    }.runOnce()

    run {
        val NUM_BARS = 10
        var percent by KonsoleVar(0)
        konsole {
            underline {
                textLine("Animated progress bar test")
            }
            text("[")
            val fullBars = NUM_BARS * percent / 100
            text("*".repeat(fullBars))
            text(" ".repeat(NUM_BARS - fullBars))
            textLine("] $percent%")
            textLine()
        }.runUntilFinished {
            while (percent < 100) {
                delay(100)
                percent = (percent + (Random.nextInt(1, 10))).coerceAtMost(100)
            }
        }
    }

    konsole {
        underline {
            textLine("Decoration test")
        }

        bold { textLine("Bolded") }
        underline { textLine("Underlined") }
        strikethrough { textLine("Struck through") }

        scopedState {
            bold()
            underline()
            textLine("Bolded and underlined")
            strikethrough {
                textLine("Bolded and underlined and struck through")
            }
        }

        textLine("Back to normal")
        textLine()
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