import com.varabyte.konsole.ansi.commands.textLine
import com.varabyte.konsole.core.input.CharKey
import com.varabyte.konsole.core.input.Keys
import com.varabyte.konsole.core.input.onKeyPressed
import com.varabyte.konsole.core.konsoleApp
import com.varabyte.konsole.core.runUntilSignal

private const val WIDTH = 20
private const val HEIGHT = 10

// Compare with: https://github.com/JakeWharton/mosaic/blob/trunk/samples/robot/src/main/kotlin/example/robot.kt
fun main() = konsoleApp {
    var x by KonsoleVar(0)
    var y by KonsoleVar(0)
    konsole {
        textLine("Use arrow keys to move the face. Press “q” to exit.")
        textLine("Position: $x, $y   World: $WIDTH, $HEIGHT")
        textLine()
        textLine(buildString {
            repeat(y) { append('\n') }
            repeat(x) { append(' ') }
            append("^_^")

            repeat(HEIGHT - y) { append('\n') }
        })
    }.runUntilSignal {
        onKeyPressed {
            when (key) {
                Keys.UP -> y = (y - 1).coerceAtLeast(0)
                Keys.DOWN -> y = (y + 1).coerceAtMost(HEIGHT)
                Keys.LEFT -> x = (x - 1).coerceAtLeast(0)
                Keys.RIGHT -> x = (x + 1).coerceAtMost(WIDTH)
                else -> {
                    (key as? CharKey)?.let { key ->
                        when (key.code) {
                            'q' -> signal()
                        }
                    }
                }
            }
        }
    }
}