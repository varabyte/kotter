import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*

fun main() = session {
    section {
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

        textLine("Normal colors")

        // nested syntax
        white(ColorLayer.BG) {
            black {
                textLine("Black on white")
                blue(ColorLayer.BG) {
                    textLine("Black on blue")
                }
                textLine("Black on white again")
            }
            red {
                textLine("Red on white")
            }
        }
        textLine("Normal colors again")
        textLine()

        // Using scoped state
        scopedState {
            red()
            textLine("Red text")
            white(ColorLayer.BG)
            textLine("Red on white")
        }
        textLine("Normal colors again")
        textLine()

        // Using reset
        blue()
        textLine("Blue text")
        black(ColorLayer.BG)
        textLine("Blue on black")
        clearColors()
        textLine("Normal colors again")
        textLine()

        // Using clear methods
        scopedState {
            bold()
            underline()
            textLine("Underlined, bolded text")
            clearBold()
            textLine("Underlined text")
            red()
            blue(ColorLayer.BG)
            textLine("Underlined red on blue text")
            clearColor(ColorLayer.BG)
            textLine("Underlined red text")
            clearUnderline()
            green(ColorLayer.BG)
            textLine("Red on green text")
            clearColors()
            textLine("Normal colors again")
            textLine()
        }

        // Using scoped clear methods
        scopedState {
            blue(ColorLayer.BG)
            red()
            textLine("Red on blue")
            clearColor {
                textLine("Default on blue")
            }
            textLine("Red on blue again")
            textLine()
        }

        // Inverting colors
        scopedState {
            blue()
            green(ColorLayer.BG)
            textLine("Blue on green")
            invert {
                textLine("Green on blue (inverted)")
            }
            textLine("Blue on green again")
            clearAll()
            textLine("Normal colors again")
            textLine()
        }

        bold {
            underline {
                strikethrough {
                    red(ColorLayer.BG) {
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

        p {
            // This is more to make sure our virtual terminal handles emojis well than anything else, really
            textLine("Emoji test: \uD83D\uDE00\uD83D\uDC4B\uD83D\uDE80")
        }

        // Test links
        p {
            text("Thank you for taking the time to ")
            link("https://github.com/varabyte/kotter", "learn Kotter")
            textLine("!")
        }
    }.run()
}