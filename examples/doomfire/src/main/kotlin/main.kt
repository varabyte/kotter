import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.addTimer
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

// Fire renderer ported from https://fabiensanglard.net/doom_fire_psx/

// Size chosen to produce a ~16:9 final gif render (in my terminal, the line height is higher than char width)
private const val VIEW_WIDTH = 70
private const val VIEW_HEIGHT = 17
private const val MAX_X = VIEW_WIDTH - 1
private const val MAX_Y = VIEW_HEIGHT - 1

enum class FireColor(val ansiColor: Color) {
    WHITE_BRIGHT(Color.BRIGHT_WHITE),
    WHITE(Color.WHITE),
    YELLOW_BRIGHT(Color.BRIGHT_YELLOW),
    YELLOW(Color.YELLOW),
    RED_BRIGHT(Color.BRIGHT_RED),
    RED(Color.RED),
    BRIGHT_BLACK(Color.BRIGHT_BLACK),
    BLACK(Color.BLACK),
    NOTHING(Color.MAGENTA); // Debug color, it should never be visible if my code logic is right

    fun cooler(): FireColor {
        return when (this) {
            NOTHING -> NOTHING
            else -> values()[ordinal + 1]
        }
    }
}

operator fun Array<FireColor>.get(x: Int, y: Int) = this[y * VIEW_WIDTH + x]
operator fun Array<FireColor>.set(x: Int, y: Int, value: FireColor) {
    this[y * VIEW_WIDTH + x] = value
}

class DoomFireModel {
    val buffer = Array(VIEW_WIDTH * VIEW_HEIGHT) { FireColor.NOTHING }
    private var isFireOn = false

    init {
        assert(!isFireOn)
        toggleFire()
    }

    fun update() {
        if (!isFireOn) {
            // Decaying the source of the fire will eventually starve the rest of it
            for (x in 0..MAX_X) {
                buffer[x, MAX_Y] = buffer[x, MAX_Y].cooler()
            }
        }

        for (y in 0 until MAX_Y) { // until: Always leave the last Y line alone, it's the source of the fire
            for (x in 0..MAX_X) {
                val srcColor = buffer[x, y + 1]
                var dstColor = FireColor.NOTHING
                var xFinal = x
                if (srcColor != FireColor.NOTHING) {
                    val shouldDecay = (Random.nextFloat() > 0.4)
                    val xOffsetRandomness = (Random.nextFloat() * 3.0).toInt() - 1 // Windy to the left
                    dstColor = if (shouldDecay) srcColor.cooler() else srcColor
                    xFinal = (x - xOffsetRandomness + VIEW_WIDTH) % VIEW_WIDTH // Wrap x
                }
                buffer[xFinal, y] = dstColor
            }
        }

    }

    fun toggleFire() {
        isFireOn = !isFireOn
        if (isFireOn) {
            for (x in 0..MAX_X) {
                buffer[x, MAX_Y] = FireColor.WHITE_BRIGHT
            }
        }
    }
}

fun main() = session(clearTerminal = true) {
    section {
        p {
            textLine("Press SPACE to toggle fire on and off")
            textLine("Press Q to quit")
        }
    }.run()

    val doomFire = DoomFireModel()
    section {
        for (y in 0..MAX_Y) {
            for (x in 0..MAX_X) {
                val fireColor = doomFire.buffer[x, y]
                if (fireColor == FireColor.NOTHING) {
                    text(" ")
                } else {
                    color(fireColor.ansiColor)
                    text("*")
                }
            }
            textLine()
        }
    }.runUntilKeyPressed(Keys.Q) {
        onKeyPressed {
            if (key == Keys.SPACE) {
                doomFire.toggleFire()
            }
        }
        addTimer(50.milliseconds, repeat = true) {
            doomFire.update()
            rerender()
        }
    }
}