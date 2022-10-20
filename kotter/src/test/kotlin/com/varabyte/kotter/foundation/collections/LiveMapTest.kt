package com.varabyte.kotter.foundation.collections

import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.kotterx.test.terminal.lines
import com.varabyte.kotterx.test.terminal.resolveRerenders
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class LiveMapTest {

    @Test
    fun `live map repaints section on change`() = testSession { terminal ->
        val numNames = liveMapOf(1 to "one", 2 to "two", 3 to "three")
        section {
            numNames.forEach { (num, name) -> textLine("$num is spelled \"$name\"") }
        }.run {
            numNames.putAll(mapOf(4 to "four", 5 to "five", 6 to "six"))
        }

        assertThat(terminal.lines()).containsExactly(
            "1 is spelled \"one\"",
            "2 is spelled \"two\"",
            "3 is spelled \"three\"",
            "${Codes.Sgr.RESET}"
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}${Codes.Cursor.MOVE_TO_PREV_LINE}".repeat(3)
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}"
                    + "1 is spelled \"one\"",
            "2 is spelled \"two\"",
            "3 is spelled \"three\"",
            "4 is spelled \"four\"",
            "5 is spelled \"five\"",
            "6 is spelled \"six\"",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }

    @Test
    fun `live map query methods work`() = testSession { terminal ->
        fun squarePair(value: Int) = value to value * value
        val numSquares = liveMapOf((1..3).map(::squarePair))
        section {
            textLine("Set: [${numSquares.entries.joinToString(", ") { (num, squared) -> "$num² = $squared"}}]")
            textLine("Keys: [${numSquares.keys.joinToString(", ")}]")
            textLine("Values: [${numSquares.values.joinToString(", ")}]")
            textLine("Size: ${numSquares.size}")
            textLine("Is empty: ${numSquares.isEmpty()}")
            textLine("Contains key: ${numSquares.containsKey(4)}")
            textLine("Contains value: ${numSquares.containsValue(25)}")
            textLine("Square of 2: ${numSquares[2]}")
        }.run {
            numSquares.putAll((4..6).map(::squarePair))
        }

        assertThat(terminal.lines()).containsExactly(
            "Set: [1² = 1, 2² = 4, 3² = 9]",
            "Keys: [1, 2, 3]",
            "Values: [1, 4, 9]",
            "Size: 3",
            "Is empty: false",
            "Contains key: false",
            "Contains value: false",
            "Square of 2: 4",
            "${Codes.Sgr.RESET}"
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}${Codes.Cursor.MOVE_TO_PREV_LINE}".repeat(8)
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}"
                    + "Set: [1² = 1, 2² = 4, 3² = 9, 4² = 16, 5² = 25, 6² = 36]",
            "Keys: [1, 2, 3, 4, 5, 6]",
            "Values: [1, 4, 9, 16, 25, 36]",
            "Size: 6",
            "Is empty: false",
            "Contains key: true",
            "Contains value: true",
            "Square of 2: 4",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }

    @Test
    fun `live map modify methods work`() = testSession { terminal ->
        val numSquares = liveMapOf<Int, Int>()
        section {
            text("Set: [${numSquares.entries.joinToString(", ") { (num, squared) -> "$num² = $squared"}}]")
        }.run {
            numSquares.withWriteLock {
                (1..10).forEach { i -> numSquares[i] = i }
                numSquares.clear()
                numSquares[0] = 123
                numSquares.remove(0)

                (1 .. 3).forEach { i -> numSquares[i] = i * i }
                numSquares.putAll((4 .. 6).map { i -> i to i * i })
            }
        }

        assertThat(terminal.resolveRerenders()).containsExactly(
            "Set: [1² = 1, 2² = 4, 3² = 9, 4² = 16, 5² = 25, 6² = 36]${Codes.Sgr.RESET}",
            "",
        ).inOrder()
    }
}