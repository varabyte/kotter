import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.ColorLayer
import com.varabyte.kotter.foundation.text.clearColor
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.p
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import java.util.*

enum class Dir {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    fun opposite(): Dir {
        return when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
    }
}

class Board(val sideLen: Int = 4) {
    inner class Pos(val x: Int, val y: Int) {
        fun toIndex() = y * sideLen + x

        fun getNeighbor(dir: Dir): Pos? {
            return when (dir) {
                Dir.UP -> if (y > 0) Pos(x, y - 1) else null
                Dir.DOWN -> if (y < sideLen - 1) Pos(x, y + 1) else null
                Dir.LEFT -> if (x > 0) Pos(x - 1, y) else null
                Dir.RIGHT -> if (x < sideLen - 1) Pos(x + 1, y) else null
            }
        }

        override fun equals(other: Any?): Boolean {
            return (other is Pos) && x == other.x && y == other.y
        }
        override fun hashCode() = Objects.hash(x, y) // Don't need this but makes a warning go away
    }

    fun pos(x: Int, y: Int) = Pos(x, y)

    var blankPos = Pos(sideLen - 1, sideLen - 1)
        private set

    private val buffer = Array(sideLen * sideLen) { i -> if (i != blankPos.toIndex()) i else null }
    private operator fun Array<Int?>.get(x: Int, y: Int) = this[y * sideLen + x]
    private operator fun Array<Int?>.get(pos: Pos) = get(pos.x, pos.y)
    private operator fun Array<Int?>.set(x: Int, y: Int, value: Int?) {
        this[y * sideLen + x] = value
    }

    private operator fun Array<Int?>.set(pos: Pos, value: Int?) {
        set(pos.x, pos.y, value)
    }

    operator fun get(pos: Pos) = buffer[pos]

    init {
        assert(isSolved()) // Board is set up correctly
    }

    fun move(dir: Dir): Boolean {
        val tileToMove = blankPos.getNeighbor(dir.opposite())
        return if (tileToMove != null) {
            val value = buffer[tileToMove]
            buffer[blankPos] = value
            buffer[tileToMove] = null
            blankPos = tileToMove
            true
        } else {
            false
        }
    }

    fun isSolved(): Boolean {
        return buffer.last() == null &&
                buffer.dropLast(1)
                    .asSequence()
                    .mapIndexed { i, value -> i == value }
                    .all { it == true }
    }


    fun randomize(numMoves: Int = 100) {
        // Ludicrously small chance we'll randomize into a finished board, but if that happens, just keep trying until
        // we succeed.
        while (isSolved()) {
            var remaining = numMoves
            while (remaining > 0) {
                if (move(Dir.values().random())) {
                    remaining--
                }
            }
        }
    }
}

private fun Int.toTileChar(): Char {
    return 'A'.plus(this)
}

/**
 * Tiles should be colored in a diagonal pattern, so the solved puzzle looks something like:
 *
 * ```
 * XOXO
 * OXOX
 * XOXO
 * OXO
 * ```
 *
 * Note that it's not just as simple as coloring tiles if they're even or odd, but it depends if they're on an
 * even or odd row.
 */
private fun Int.isAltPattern(sideLen: Int): Boolean {
    val x = this % sideLen
    val y = this / sideLen
    return if (y % 2 == 0) {
        (x % 2 == 1)
    } else {
        (x % 2 == 0)
    }
}

private fun createRandomizedBoard() = Board().apply { randomize() }

fun main() = session {
    var board = createRandomizedBoard()

    section {
        p {
            textLine("Press arrow keys to \"fill\" in the blank space.")
            textLine("Q to quit, R to restart")
        }
    }.run()
    section {
        val isFinished = board.isSolved()

        // Note: tiles are 5x3 pixels (fonts are taller than wider, so this actually looks "square"ish)
        for (lineY in (0 until board.sideLen * 3)) {
            val tileY = lineY / 3
            for (tileX in (0 until board.sideLen)) {
                val tilePos = board.pos(tileX, tileY)
                val tile = board[tilePos]
                if (tile != null) {
                    if (tile.isAltPattern(board.sideLen)) {
                        red(ColorLayer.BG); white(ColorLayer.FG)
                    } else {
                        white(ColorLayer.BG); red(ColorLayer.FG)
                    }
                    when (lineY % 3) {
                        0 -> {
                            val up = "↑".takeIf { !isFinished && tilePos.getNeighbor(Dir.UP) == board.blankPos } ?: " "
                            text("  $up  ")
                        }
                        1 -> {
                            val left = "←".takeIf { !isFinished && tilePos.getNeighbor(Dir.LEFT) == board.blankPos } ?: " "
                            val right = "→".takeIf { !isFinished && tilePos.getNeighbor(Dir.RIGHT) == board.blankPos } ?: " "
                            text("$left ${tile.toTileChar()} $right")
                        }
                        2 -> {
                            val down = "↓".takeIf { !isFinished && tilePos.getNeighbor(Dir.DOWN) == board.blankPos } ?: " "
                            text("  $down  ")
                        }
                    }
                } else {
                    clearColor(ColorLayer.BG)
                    text(" ".repeat(5))
                }
            }
            textLine()
        }

        if (isFinished) {
            textLine()
            cyan { textLine("Play again? (Y/n)") }
        }
        textLine()
    }.runUntilSignal {
        onKeyPressed {
            var handled = true
            if (!board.isSolved()) {
                when (key) {
                    Keys.UP -> board.move(Dir.UP)
                    Keys.DOWN -> board.move(Dir.DOWN)
                    Keys.LEFT -> board.move(Dir.LEFT)
                    Keys.RIGHT -> board.move(Dir.RIGHT)
                    Keys.R -> board = createRandomizedBoard()
                    Keys.Q -> signal()
                    else -> handled = false
                }
            }
            else {
                when (key) {
                    Keys.Y -> board = createRandomizedBoard()
                    Keys.N, Keys.Q -> signal()
                    else -> handled = false
                }
            }
            if (handled) {
                rerender()
            }
        }
    }
}