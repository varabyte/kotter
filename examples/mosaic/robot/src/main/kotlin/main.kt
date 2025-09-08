import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*

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
                Keys.Up -> y = (y - 1).coerceAtLeast(0)
                Keys.Down -> y = (y + 1).coerceAtMost(HEIGHT)
                Keys.Left -> x = (x - 1).coerceAtLeast(0)
                Keys.Right -> x = (x + 1).coerceAtMost(WIDTH)
            }
        }
    }
}
