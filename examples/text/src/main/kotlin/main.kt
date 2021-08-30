import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.foundation.text.*
import com.varabyte.konsole.foundation.text.ColorLayer.BG
import kotlinx.coroutines.delay
import kotlin.random.Random

fun main() = konsoleApp {
    konsole {
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

        textLine("No colors")

        // nested syntax
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
        textLine()

        // Using scoped state
        scopedState {
            red()
            textLine("Red text")
            white(BG)
            textLine("Red on white")
        }
        textLine("No colors again")
        textLine()

        // Using reset
        blue()
        textLine("Blue text")
        black(BG)
        textLine("Blue on black")
        reset()
        textLine("No colors again")
        textLine()

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
            textLine()
        }

        bold {
            underline {
                strikethrough {
                    red(BG) {
                        blue {
                            invert {
                                textLine("All styles applied! (Red on blue)")
                            }
                        }
                    }
                }
            }
        }
        textLine("Back to default again")
        textLine()
    }.run()
}