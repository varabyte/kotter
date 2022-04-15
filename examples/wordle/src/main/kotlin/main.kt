import com.varabyte.kotter.foundation.anim.textAnimOf
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.addTimer
import com.varabyte.kotter.runtime.Section
import com.varabyte.kotter.runtime.concurrent.createKey
import com.varabyte.kotter.runtime.render.RenderScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration

private const val URL_BASE = "https://raw.githubusercontent.com/varabyte/media/main/kotter/data"
private const val WORD_LEN = 5
private const val MAX_GUESSES = 6
private val KEYBOARD_LETTERS = listOf(
    "QWERTYUIOP".toList(),
    "ASDFGHJKL".toList(),
    " ZXCVBNM".toList(),
)

private enum class GameMode {
    NORMAL,
    HARD;

    fun toggle(): GameMode {
        return when(this) {
            NORMAL -> HARD
            HARD -> NORMAL
        }
    }
}

// High contrast controls what tile colors are rendered
private val HighContrastKey = Section.Lifecycle.createKey<Boolean>()

@Suppress("EqualsOrHashCode") // hashcode override not needed for this example
private class Tile(val type: Type, letter: Char) {
    val letter: Char = letter.uppercaseChar()
    enum class Type {
        ABSENT,
        PRESENT,
        MATCH
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Tile) return false
        return this.type == other.type && this.letter == other.letter
    }
}

@Suppress("EqualsOrHashCode") // hashcode override not needed for this example
private class Row(val tiles: Array<Tile>) {
    init {
        require(tiles.size == WORD_LEN)
    }
    companion object {
        @Suppress("NAME_SHADOWING")
        fun from(guess: String, targetWord: String): Row {
            // Force lowercase to make sure we never have a case-comparison issue
            val guess = guess.lowercase()
            val targetWord = targetWord.lowercase()
            require(guess.length == targetWord.length && targetWord.length == WORD_LEN)
            val tiles = Array<Tile?>(WORD_LEN) { null }
            val mutableTargetWord = Array<Char?>(WORD_LEN) { i -> targetWord[i] }
            // Run through and remove matches first
            for (i in guess.indices) {
                val guessLetter = guess[i]
                if (guessLetter == mutableTargetWord[i]) {
                    tiles[i] = Tile(Tile.Type.MATCH, guessLetter)
                    mutableTargetWord[i] = null // Don't let this match get counted in PRESENT check
                }
            }

            // Next, finish search with present and absent characters
            for (i in guess.indices) {
                if (tiles[i] == null) {
                    val guessLetter = guess[i]
                    val presentIndex = mutableTargetWord.indexOf(guessLetter)
                    if (presentIndex >= 0) {
                        tiles[i] = Tile(Tile.Type.PRESENT, guessLetter)
                        mutableTargetWord[presentIndex] = null
                    } else {
                        tiles[i] = Tile(Tile.Type.ABSENT, guessLetter)
                    }
                }
            }

            return Row(tiles.filterNotNull().toTypedArray())
        }
    }

    val word = tiles.joinToString("") { it.letter.toString() }.lowercase()

    override fun equals(other: Any?): Boolean {
        if (other !is Row) return false
        return (0 until WORD_LEN).all { i -> this.tiles[i] == other.tiles[i] }
    }
}

private class Board(val rows: Array<Row>, val targetWord: String) {
    enum class EndState {
        WON,
        LOST,
    }

    val endState: EndState? =
        if (rows.lastOrNull()?.word == targetWord) EndState.WON
        else if (rows.size == MAX_GUESSES) EndState.LOST
        else null

    operator fun plus(row: Row) = Board(rows + row, targetWord)
}

private class UserStats {
    var currStreak: Int = 0

    var numWins: Int = 0
        private set

    var numLosses: Int = 0
        private set

    var winGuessAverage: Double = 0.0
        private set

    fun markLoss() {
        ++numLosses
        currStreak = 0
    }

    fun markWin(numGuesses: Int) {
        ++numWins
        ++currStreak
        winGuessAverage = ((winGuessAverage * (numWins - 1)) + numGuesses) / numWins
    }
}

private sealed interface GameState {
    object Downloading : GameState
    class Playing(val board: Board, val error: String? = null) : GameState
    class Revealing(val board: Board, val row: Row, val numTiles: Int) : GameState {
        fun revealOneMore(): Revealing? = if (numTiles < WORD_LEN) Revealing(board, row, numTiles + 1) else null
    }
    class EndGame(val board: Board) : GameState
}

private fun RenderScope.setColorFor(type: Tile.Type) {
    val useHighContrast = data.getValue(HighContrastKey)
    if (!useHighContrast) {
        when (type) {
            Tile.Type.ABSENT -> rgb(0x555555, ColorLayer.BG)
            Tile.Type.PRESENT -> { rgb(0xb59f3b, ColorLayer.BG); black() }
            Tile.Type.MATCH -> { rgb(0x538d4e, ColorLayer.BG); black() }
        }
    }
    else {
        when (type) {
            Tile.Type.ABSENT -> rgb(0x555555, ColorLayer.BG)
            Tile.Type.PRESENT -> { rgb(0x85c0f9, ColorLayer.BG); black() }
            Tile.Type.MATCH -> { rgb(0xf5793a, ColorLayer.BG); black() }
        }
    }
}

private fun RenderScope.renderRow(row: Row, indent: Int = 0, numTilesWithColor: Int = row.tiles.size) {
    text(" ".repeat(indent))
    row.tiles.take(numTilesWithColor).forEach { tile ->
        scopedState {
            setColorFor(tile.type)
            text(tile.letter)
        }
    }
    row.tiles.drop(numTilesWithColor).forEach { tile ->
        text(tile.letter)
    }

    textLine()
}


private fun RenderScope.renderBoard(board: Board, indent: Int = 0) {
    board.rows.forEach { row -> renderRow(row, indent) }
}

private fun RenderScope.renderKeyboard(board: Board) {
    textLine()
    val tileTypes = mutableMapOf<Char, Tile.Type>()
    board.rows.forEach { row ->
        row.tiles.forEach { tile ->
            tileTypes.compute(tile.letter) { _, existingType ->
                // Tile types with stronger guarantees (e.g. match over present) always take precedence
                if (existingType == null || tile.type.ordinal > existingType.ordinal) tile.type else existingType
            }
        }
    }

    KEYBOARD_LETTERS.forEach { row ->
        row.forEach { letter ->
            scopedState {
                tileTypes[letter]?.let { setColorFor(it) }
                text(letter)
            }
        }
        textLine()
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

private fun RenderScope.renderUserStats(userStats: UserStats) {
    text("W: "); text(userStats.numWins.toString()); text(" | ")
    text("L: "); text(userStats.numLosses.toString()); text(" | ")
    text("AVG GUESSES: ${userStats.winGuessAverage.format(2)}"); text(" | ")
    text("STREAK: ${userStats.currStreak}")
    textLine()
}

private fun RenderScope.toOnOff(value: Boolean) {
    text("[")
    if (value) {
        green { text("On") }
    }
    else {
        text("Off")
    }
    text("]")
}

fun main() = session {
    val waitingAnim = textAnimOf(listOf("", ".", "..", "..."), frameDuration = Duration.ofMillis(250))
    var gameState by liveVarOf<GameState>(GameState.Downloading)
    var gameMode by liveVarOf(GameMode.NORMAL)
    var useHighContrast by liveVarOf(false)
    var showStats by liveVarOf(false)
    var showInstructions by liveVarOf(false)
    val userStats = UserStats()

    section {
        data[HighContrastKey] = useHighContrast

        textLine()
        bold { textLine("WORDLE DEMAKE") }
        textLine()

        if (gameState == GameState.Downloading) {
            textLine("Fetching words$waitingAnim")
        } else {
            textLine("To toggle, press...")
            text("  1: Hard Mode "); toOnOff(gameMode == GameMode.HARD); textLine()
            text("  2: High Contrast "); toOnOff(useHighContrast); textLine()
            text("  0: Show Stats "); toOnOff(showStats); textLine()
            text("  ?: Show Instructions "); toOnOff(showInstructions); textLine()
            textLine()
            if (showInstructions) {
                textLine("You have six tries to guess a five-letter word.")
                textLine("After each guess, tiles will be colored to indicate how close you were:")
                textLine()
                text(" ")
                scopedState {
                    setColorFor(Tile.Type.MATCH)
                    text(" ")
                }
                textLine(" A match. This letter is in the word and in the correct spot.")
                text(" ")
                scopedState {
                    setColorFor(Tile.Type.PRESENT)
                    text(" ")
                }
                textLine(" A near hit. This letter is in the word but in a different spot.")
                text(" ")
                scopedState {
                    setColorFor(Tile.Type.ABSENT)
                    text(" ")
                }
                textLine(" A miss. This letter is not in the word in any spot.")
                textLine()
            }
            if (showStats) {
                renderUserStats(userStats)
                textLine()
            }
            // Copy gameState into val so smart cast can work
            when (val gs = gameState) {
                is GameState.Playing -> {
                    // Slightly indented so we can highlight the current row with a leading "> "
                    renderBoard(gs.board, indent = 2)
                    text("> "); input()
                    textLine()
                    renderKeyboard(gs.board)
                    gs.error?.let { error ->
                        textLine()
                        textLine()
                        yellow { textLine(error) }
                    }
                }
                is GameState.Revealing -> {
                    renderBoard(gs.board, indent = 2)
                    renderRow(gs.row, indent = 2, gs.numTiles)
                    renderKeyboard(gs.board)
                }
                is GameState.EndGame -> {
                    renderBoard(gs.board, indent = 2)
                    renderKeyboard(gs.board)
                    textLine()

                    val youWon = gs.board.rows.last().tiles.all { it.type == Tile.Type.MATCH }
                    if (youWon) {
                        textLine("Congratulations! You won using ${gs.board.rows.size} guess(es).")
                    } else {
                        textLine("Sorry, you lost this round. The word was \"${gs.board.targetWord}\".")
                    }
                    textLine()
                    textLine("Would you like to play again?")
                    textLine()
                    text("> "); input(completer = Completions("yes", "no"), "y")
                }
                else -> error("Unexpected gameState: $gs")
            }
        }
    }.runUntilSignal {
        // Wordle has a permissive list of words that the user can use and a more restrictive set that
        // it can choose solutions from.
        val (allWords, commonWords) = withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val client = OkHttpClient()
            val allWords = async {
                val request = Request.Builder().url("$URL_BASE/all-words.txt").build()
                client.newCall(request).execute().use { response ->
                    response.body!!.string().lines().toSet()
                }
            }

            val commonWords = async {
                val request = Request.Builder().url("$URL_BASE/common-words.txt").build()
                client.newCall(request).execute().use { response ->
                    response.body!!.string().lines().toSet()
                }
            }

            listOf(allWords.await(), commonWords.await())
        }

        fun createNewGameState(): GameState.Playing {
            return GameState.Playing(Board(emptyArray(), commonWords.random()))
        }

        onKeyPressed {
            when (key) {
                Keys.DIGIT_0 -> showStats = !showStats
                Keys.DIGIT_1 -> gameMode = gameMode.toggle()
                Keys.DIGIT_2 -> useHighContrast = !useHighContrast
                Keys.QUESTION_MARK -> showInstructions = !showInstructions
            }

            // Clear any error whenever the player interacts with the board
            (gameState as? GameState.Playing)?.let { playing ->
                if (playing.error != null) {
                    gameState = GameState.Playing(playing.board)
                }
            }
        }

        onInputChanged {
            input = input.filter { it.isLetter() }
            if (gameState is GameState.Playing) {
                input = input.uppercase()
            }
        }

        onInputEntered {
            // Copy gameState into val so smartcast can work
            when (val gs = gameState) {
                is GameState.Playing -> {
                    if (!allWords.contains(input.lowercase())) {
                        gameState = GameState.Playing(
                            gs.board,
                            if (input.length < WORD_LEN) "The word \"$input\" is too short"
                            else if (input.length > WORD_LEN) "The word \"$input\" is too long"
                            else "The word \"$input\" is not valid"
                        )
                    } else {
                        if (gameMode == GameMode.HARD) {
                            // Extra constraint -- all previous rows, when tested against this word AS IF it were the
                            // final word, should look exactly the same as if it were the final word
                            gs.board.rows.firstOrNull { row -> Row.from(row.word, input) != row }?.run {
                                val ideal = this
                                val actual = Row.from(ideal.word, input)
                                // For this row, the ideal and actual states don't match. Find the first one that's
                                // problematic and report it
                                for (i in ideal.tiles.indices) {
                                    val idealType = ideal.tiles[i].type
                                    val actualType = actual.tiles[i].type
                                    if (idealType != actualType) {
                                        val error = when {
                                            idealType == Tile.Type.MATCH -> {
                                                "The letter ${ideal.tiles[i].letter} should be in spot #${i + 1}"
                                            }
                                            idealType == Tile.Type.PRESENT && actualType == Tile.Type.ABSENT -> {
                                                "The letter ${ideal.tiles[i].letter} should be present in your guess"
                                            }
                                            idealType == Tile.Type.PRESENT && actualType == Tile.Type.MATCH -> {
                                                "The letter ${ideal.tiles[i].letter} should not be in spot #${i + 1}"
                                            }
                                            idealType == Tile.Type.ABSENT -> {
                                                "The letter ${ideal.tiles[i].letter} should not be present in your guess"
                                            }
                                            else -> error("Unexpected case comparing $idealType to $actualType")
                                        }
                                        gameState = GameState.Playing(gs.board, "Hard mode: $error")
                                        return@onInputEntered
                                    }
                                }
                                error("Shouldn't have gotten here! We should have returned out already")
                            }
                        }
                        clearInput()

                        val newRow = Row.from(input, gs.board.targetWord)
                        addTimer(Duration.ofMillis(300), repeat = true) {
                            val revealing = gameState as? GameState.Revealing ?: run {
                                repeat = false
                                return@addTimer
                            }
                            revealing.revealOneMore()?.let { gameState = it } ?: run {
                                val newBoard = revealing.board + revealing.row
                                gameState = newBoard.endState?.let { endState ->
                                    when (endState) {
                                        Board.EndState.WON -> userStats.markWin(numGuesses = newBoard.rows.size)
                                        Board.EndState.LOST -> userStats.markLoss()
                                    }
                                    GameState.EndGame(newBoard)
                                } ?: GameState.Playing(newBoard)

                                repeat = false
                            }
                        }
                        gameState = GameState.Revealing(gs.board, newRow, 1)
                    }
                }
                is GameState.EndGame -> {
                    if ("yes".startsWith(input, ignoreCase = true)) {
                        gameState = createNewGameState()
                        clearInput()
                    } else {
                        signal()
                    }
                }
                else -> error("No input expected during game state: $gs")
            }
        }

        gameState = createNewGameState()
    }
}