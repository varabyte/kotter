import com.varabyte.konsole.konsole
import com.varabyte.konsole.text.textLine
import kotlinx.coroutines.delay

fun main() {
    run {
        var count = 0
        konsole {
            textLine("$count: " + "*".repeat(count))
        }.runUntilFinished {
            rerenderOnFinished = false
            while (count < 10) {
                ++count
                rerender()
                delay(250)
            }
        }
    }

//    konsole {
//        p {
//            green(BG) {
//                text("WELCOME TO KONSOLE!")
//            }
//            newLine()
//        }
//    }.runOnce()
//
//    konsole {
//        underline {
//            textLine("Nested colors test")
//        }
//        textLine("No colors")
//        white(BG) {
//            black {
//                textLine("Black on white")
//                blue(BG) {
//                    textLine("Black on blue")
//                }
//                textLine("Black on white again")
//            }
//            red {
//                textLine("Red on white")
//            }
//        }
//        textLine("No colors again")
//        newLine()
//    }.runOnce()
//
//    konsole {
//        underline {
//            textLine("clearColors test")
//        }
//
//        red()
//        textLine("Red text")
//        white(BG)
//        textLine("Red on white")
//        clearColors()
//        textLine("Text back to normal")
//    }.runOnce()
}