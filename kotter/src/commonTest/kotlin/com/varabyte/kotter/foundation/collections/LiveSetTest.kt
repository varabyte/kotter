package com.varabyte.kotter.foundation.collections

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class LiveSetTest {

    @Test
    fun `live set repaints section on change`() = testSession { terminal ->
        val nums = liveSetOf(1, 2, 3)
        section {
            nums.forEach { num -> textLine(num.toString()) }
        }.run {
            nums.addAll(listOf(4, 5, 6))
        }

        assertThat(terminal.lines()).containsExactly(
            "1",
            "2",
            "3",
            "${Codes.Sgr.RESET}"
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}${Codes.Cursor.MOVE_TO_PREV_LINE}".repeat(3)
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }

    @Test
    fun `live set query methods work`() = testSession { terminal ->
        val nums = liveSetOf<Int>()
        section {
            textLine("Set: [${nums.joinToString(", ")}]")
            textLine("Size: ${nums.size}")
            textLine("Contains: ${nums.contains(2)}")
            textLine("Contains All: ${nums.containsAll(listOf(1, 3, 2))}")
            textLine("Is empty: ${nums.isEmpty()}")
        }.run {
            nums.addAll(listOf(1, 2, 3))
        }

        assertThat(terminal.lines()).containsExactly(
            "Set: []",
            "Size: 0",
            "Contains: false",
            "Contains All: false",
            "Is empty: true",
            "${Codes.Sgr.RESET}"
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}${Codes.Cursor.MOVE_TO_PREV_LINE}".repeat(5)
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}"
                    + "Set: [1, 2, 3]",
            "Size: 3",
            "Contains: true",
            "Contains All: true",
            "Is empty: false",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }

    @Test
    fun `live set iterator method works`() = testSession { terminal ->
        val nums = liveSetOf(listOf(1, 2, 3, 4, 5, 6))
        section {
            text("Iterator: ")
            with(nums.iterator()) {
                while (hasNext()) {
                    text(next().toString())
                    if (hasNext()) {
                        text(", ")
                    }
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Iterator: 1, 2, 3, 4, 5, 6${Codes.Sgr.RESET}",
            "",
        ).inOrder()
    }

    @Test
    fun `live set modify methods work`() = testSession { terminal ->
        val nums = liveSetOf<Int>()
        section {
            text(nums.joinToString(", "))
        }.run {
            nums.withWriteLock {
                nums.add(123)
                nums.add(456)
                nums.add(789)
                nums.clear()

                nums.addAll(listOf(1, 2, 3))

                nums.add(123)
                nums.add(456)
                nums.add(789)
                nums.remove(123)
                nums.removeAll(listOf(456, 789))

                nums.add(123)
                nums.add(456)
                nums.add(789)
                nums.retainAll(listOf(1, 2, 3))
            }
        }

        terminal.assertMatches {
            text("1, 2, 3")
        }
    }
}
