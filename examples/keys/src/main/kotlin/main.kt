import com.varabyte.konsole.foundation.input.CharKey
import com.varabyte.konsole.foundation.input.Keys
import com.varabyte.konsole.foundation.input.onKeyPressed
import com.varabyte.konsole.foundation.input.runUntilKeyPressed
import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.foundation.text.textLine

fun main() = konsoleApp {
    var keyName by konsoleVarOf("")
    konsole {
        textLine("Press any key (and Q will quit)")
        textLine()
        textLine("Last key pressed: $keyName")
    }.runUntilKeyPressed(Keys.Q) {
        onKeyPressed {
            keyName = when (key) {
                Keys.ESC -> "ESC"
                Keys.ENTER -> "ENTER"
                Keys.BACKSPACE -> "BACKSPACE"
                Keys.DELETE -> "DELETE"
                Keys.EOF -> "EOF"
                Keys.UP -> "UP"
                Keys.DOWN -> "DOWN"
                Keys.LEFT -> "LEFT"
                Keys.RIGHT -> "RIGHT"
                Keys.HOME -> "HOME"
                Keys.END -> "END"
                Keys.INSERT -> "INSERT"
                Keys.PAGE_UP -> "PAGE_UP"
                Keys.PAGE_DOWN -> "PAGE_DOWN"
                Keys.TAB -> "TAB"
                is CharKey -> (key as CharKey).code.toString()
                else -> "Unhandled key. Please report a bug!"
           }
        }
    }
}