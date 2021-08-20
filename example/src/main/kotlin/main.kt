import com.varabyte.konsole.konsole
import com.varabyte.konsole.text.*
import com.varabyte.konsole.text.ColorLayer.BG

fun main() {
    konsole {
        textLine("Hello World")
        white(BG) {
            red {
                text("Hello")
            }
            text(" ")
            blue {
                text("World")
            }
        }
    }
}