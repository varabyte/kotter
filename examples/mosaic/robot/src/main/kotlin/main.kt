import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.textLine

private const val WIDTH = 20
private const val HEIGHT = 10

// Compare with: https://github.com/JakeWharton/mosaic/blob/trunk/samples/robot/src/main/kotlin/example/robot.kt
fun main() = session {
    var x by liveVarOf(0)
    var y by liveVarOf(0)
    section {
        textLine("Use arrow keys to move the face. Press Q to quit.")
        textLine("Position: $x, $y   World: $WIDTH, $HEIGHT")
        textLine()
        textLine(buildString {
            repeat(y) { append('\n') }
            repeat(x) { append(' ') }
            append("^_^")

            repeat(HEIGHT - y) { append('\n') }
        })
    }.runUntilKeyPressed(Keys.Q) {
        onKeyPressed {
            when (key) {
                Keys.UP -> y = (y - 1).coerceAtLeast(0)
                Keys.DOWN -> y = (y + 1).coerceAtMost(HEIGHT)
                Keys.LEFT -> x = (x - 1).coerceAtLeast(0)
                Keys.RIGHT -> x = (x + 1).coerceAtMost(WIDTH)
            }
        }
    }
}