import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.collections.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.terminal.system.*

fun main() = session(SystemTerminal()) { //exclude VirtualTerminal for native image
    val keys = liveListOf<String>()
    section {
        text("Press any key (and "); bold { text('Q') }; textLine(" will quit)")
        keys.distinct().run {
            textLine("Distinct keys pressed: $size [${joinToString(" ")}]")
        }
        textLine("All keys pressed: ${keys.size} [${keys.joinToString(" ")}]")
    }.runUntilKeyPressed(Keys.Q) {
        onKeyPressed {
            when (key) {
                Keys.Space -> "Space"
                else       -> "$key"
            }.let { keys.add(it) }
        }
    }
}
