import com.varabyte.kotter.foundation.collections.liveListOf
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.text.ColorLayer.BG
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session

fun main() = session {
//    inputExample()
    textExample()
}

private fun Session.inputExample() {
    // Scenario #1 - trivial but common case. A section exists to request a single input from the user
    run {
        var wantsToLearn by liveVarOf(false)
        section {
            text("Would you like to learn "); cyan { text("Kotter") }; textLine("? (Y/n)")
            text("> "); input(Completions("yes", "no"), initialText = "y")

            if (wantsToLearn) {
                yellow(isBright = true) { p { textLine("""\(^o^)/""") } }
            }
        }.runUntilInputEntered {
            onInputEntered { wantsToLearn = "yes".startsWith(input.lowercase()) }
        }

        if (!wantsToLearn) return
    }

    // Scenario #2 - Recycling the same input to request multiple values.
    run {
        val numbers = liveListOf<Int>()
        var isDone by liveVarOf(false)

        section {
            cyan()
            textLine("Keeping typing numbers and we'll calculate the sum of them all.")
            textLine("Press ENTER on an empty line to finish.")
            textLine()
        }.run()

        section {
            if (!isDone) {
                numbers.forEach { num ->
                    textLine("   $num")
                }
                text(" + "); input(); textLine()
            } else {
                numbers.forEachIndexed { i, num ->
                    if (i < numbers.lastIndex) {
                        text("   ")
                    } else {
                        text(" + ")
                    }
                    textLine("$num")
                }
            }
            val sum = numbers.sum()
            textLine("-".repeat(sum.toString().length + 3))
            textLine("   $sum")
            textLine()
        }.runUntilSignal {
            onInputChanged { if (input.isNotBlank() && input.toIntOrNull() == null) rejectInput() }
            onInputEntered {
                numbers.add(input.toIntOrNull() ?: 0)
                if (input.isEmpty()) { isDone = true; signal() }
                clearInput() // Clear the input, so we can start collecting the next number
            }
        }
    }

    // Scenario #3 - Multiple inputs in the same block. Only one can be active at a time!
    run {
        section {
            cyan()
            textLine("Type in RGB values to choose a color.")
            textLine("Colors must be in the range 0-255.")
            textLine("Press enter to confirm a color value.")
            textLine("Use arrows to navigate between fields.")
            textLine("Press ESC when finished.")
            textLine()
        }.run()

        var selectedLine by liveVarOf(0)
        var warning by liveVarOf<String?>(null)
        val colors = liveListOf(0, 128, 255)

        // Has to extend "MainRenderScope" and not just "RenderScope" for access to the "input" function
        fun MainRenderScope.colorInput(line: Int, prompt: String) {
            scopedState {
                if (selectedLine == line) bold()
                text("$prompt: ")
                input(id = line, initialText = colors[line].toString(), isActive = selectedLine == line)
                textLine()
            }
        }

        // To allow for multiple inputs in the same block, they must all have unique IDs.
        // IDs can be any value -- use whatever you want! Here, we just use the current line number as each input's ID.
        section {
            colorInput(line = 0, "R")
            colorInput(line = 1, "G")
            colorInput(line = 2, "B")
            text("Result: ")
            scopedState {
                rgb(colors[0], colors[1], colors[2])
                invert()
                textLine("COLOR")
            }
            textLine()
            warning?.let { warning ->
                yellow { textLine(warning) }
                textLine()
            }
        }.runUntilSignal {
            onInputChanged { if (input.isNotBlank() && input.toIntOrNull() == null) rejectInput() }
            onInputEntered {
                warning = null
                val num = input.toIntOrNull() ?: 0
                if (num in 0..255) {
                    colors[selectedLine] = num
                } else {
                    warning = "Color values must be between 0 and 255."
                }
            }
            onInputDeactivated {
                val line = id as Int
                input = colors[line].toString()
            }
            onKeyPressed {
                when (key) {
                    Keys.UP -> selectedLine = (selectedLine + 3 - 1) % 3
                    Keys.DOWN -> selectedLine = (selectedLine + 1) % 3
                    Keys.ESC -> signal()
                }
            }
        }
    }

    // Use input's `transform` callback to support masking a password
    run {
        fun checkPassword(password: String): List<String> = buildList {
            val hasUpper = password.any { it.isUpperCase() }
            val hasLower = password.any { it.isLowerCase()}
            val hasDigit = password.any { it.isDigit() }
            val hasSymbol = password.any { !(it.isWhitespace() || it.isLetterOrDigit()) }

            if (password.length < 8) {
                add("Your password must be at least 8 characters long")
            }

            if (!(hasUpper && hasLower)) {
                add("Your password must contain both upper and lowercase letters")
            }

            if (!hasDigit) {
                add("Your password must contain at least one digit")
            }

            if (!hasSymbol) {
                add("Your password must contain at least one symbol")
            }
        }

        var password = ""
        var passwordErrors by liveVarOf(checkPassword(password))
        var maskPassword by liveVarOf(true)

        section {
            black(isBright = true) { textLine("Press TAB to toggle showing / hiding the password") }
            textLine()
            text("Create a password: ")
            input(viewMap = { if (maskPassword) '*' else ch })
            textLine()

            textLine()
            if (passwordErrors.isNotEmpty()) {
                red {
                    textLine("Your password has the following errors:")
                    passwordErrors.forEach { error ->
                        textLine("* $error")
                    }
                }
            } else {
                green {
                    textLine("Valid password! Press ENTER to accept it.")
                }
            }
        }.runUntilSignal {
            onInputChanged {
                input = input.filter { !it.isWhitespace() }
                password = input
                passwordErrors = checkPassword(password)
            }
            onInputEntered {
                if (passwordErrors.isEmpty()) {
                    signal()
                }
            }
            onKeyPressed {
                if (key == Keys.TAB) {
                    maskPassword = !maskPassword
                } else if (key == Keys.ESC) {
                    password = ""
                    signal()
                }
            }
        }

        if (password.isNotBlank() && maskPassword) {
            section {
                textLine()
                textLine("The password you chose was: $password")
            }.run()
        }
    }
}

private fun Session.textExample() {
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
        white(com.varabyte.kotter.foundation.text.ColorLayer.BG) {
            black {
                textLine("Black on white")
                blue(com.varabyte.kotter.foundation.text.ColorLayer.BG) {
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
            white(com.varabyte.kotter.foundation.text.ColorLayer.BG)
            textLine("Red on white")
        }
        textLine("Normal colors again")
        textLine()

        // Using reset
        blue()
        textLine("Blue text")
        black(com.varabyte.kotter.foundation.text.ColorLayer.BG)
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
            blue(com.varabyte.kotter.foundation.text.ColorLayer.BG)
            textLine("Underlined red on blue text")
            clearColor(com.varabyte.kotter.foundation.text.ColorLayer.BG)
            textLine("Underlined red text")
            clearUnderline()
            green(com.varabyte.kotter.foundation.text.ColorLayer.BG)
            textLine("Red on green text")
            clearColors()
            textLine("Normal colors again")
            textLine()
        }

        // Using scoped clear methods
        scopedState {
            blue(com.varabyte.kotter.foundation.text.ColorLayer.BG)
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
            green(com.varabyte.kotter.foundation.text.ColorLayer.BG)
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
                    red(com.varabyte.kotter.foundation.text.ColorLayer.BG) {
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