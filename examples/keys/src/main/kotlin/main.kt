import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*

fun main() = session {
    var keyName by liveVarOf("")
    section {
        textLine("Press any key (and Q will quit)")
        textLine()
        textLine("Last key pressed: $keyName")
    }.runUntilKeyPressed(Keys.Q) {
        onKeyPressed {
            keyName = when (key) {
                Keys.Escape -> "ESC"
                Keys.Enter -> "ENTER"
                Keys.Backspace -> "BACKSPACE"
                Keys.Delete -> "DELETE"
                Keys.Eof -> "EOF"
                Keys.Up -> "UP"
                Keys.Down -> "DOWN"
                Keys.Left -> "LEFT"
                Keys.Right -> "RIGHT"
                Keys.Home -> "HOME"
                Keys.End -> "END"
                Keys.Insert -> "INSERT"
                Keys.PageUp -> "PAGE_UP"
                Keys.PageDown -> "PAGE_DOWN"
                Keys.Tab -> "TAB"
                Keys.Space -> "SPACE"
                is CharKey -> (key as CharKey).char.toString()
                else -> "Unhandled key. Please report a bug!"
            }
        }
    }
}
