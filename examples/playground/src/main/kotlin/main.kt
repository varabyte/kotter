import com.varabyte.konsole.ansi.commands.*
import com.varabyte.konsole.ansi.commands.ColorLayer.BG
import com.varabyte.konsole.core.KonsoleVar
import com.varabyte.konsole.konsole
import kotlinx.coroutines.delay
import kotlin.random.Random

fun main() {
    konsole {
        bold { textLine("WELCOME TO KONSOLE!") }
        textLine()
    }.run()

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
        }.run {
            while (percent < 100) {
                delay(100)
                percent = (percent + (Random.nextInt(1, 20))).coerceAtMost(100)
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
    }.run()

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

        // Using scoped state
        scopedState {
            red()
            textLine("Red text")
            white(BG)
            textLine("Red on white")
        }
        textLine("No colors again")

        // Using reset
        blue()
        textLine("Blue text")
        black(BG)
        textLine("Blue on black")
        reset()
        textLine("No colors again")

        // Inverting colors
        scopedState {
            blue()
            green(BG)
            textLine("Blue on green")
            invert {
                textLine("Green on blue (inverted)")
            }
            textLine("Blue on green again")
            reset()
            textLine("No colors again")
        }
    }.run()
}