package com.varabyte.kotter.foundation.collections

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.terminal.mock.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class LiveListTest {

    @Test
    fun `live list repaints section on change`() = testSession { terminal ->
        val nums = liveListOf(1, 2, 3)
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
    fun `live list query methods work`() = testSession { terminal ->
        val nums = liveListOf(listOf(1, 2, 6))
        section {
            textLine("List: [${nums.joinToString(", ")}]")
            textLine("Size: ${nums.size}")
            textLine("Contains: ${nums.contains(5)}")
            textLine("Contains All: ${nums.containsAll(listOf(1, 3, 5))}")
            textLine("2nd element: ${nums[1]}")
            textLine("Index of: ${nums.indexOf(3)}")
            textLine("Is empty: ${nums.isEmpty()}")
            textLine("Last index of: ${nums.lastIndexOf(3)}")
            textLine("Sublist: ${nums.subList(0, 2)}")
        }.run {
            nums.withWriteLock {
                nums.addAll(2, listOf(3, 4, 5))
                nums.add(3) // Add a second three, to test "lastIndexOf"
            }
        }

        assertThat(terminal.lines()).containsExactly(
            "List: [1, 2, 6]",
            "Size: 3",
            "Contains: false",
            "Contains All: false",
            "2nd element: 2",
            "Index of: -1",
            "Is empty: false",
            "Last index of: -1",
            "Sublist: [1, 2]",
            "${Codes.Sgr.RESET}"
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}${Codes.Cursor.MOVE_TO_PREV_LINE}".repeat(9)
                    + "\r${Codes.Erase.CURSOR_TO_LINE_END}"
                    + "List: [1, 2, 3, 4, 5, 6, 3]",
            "Size: 7",
            "Contains: true",
            "Contains All: true",
            "2nd element: 2",
            "Index of: 2",
            "Is empty: false",
            "Last index of: 6",
            "Sublist: [1, 2]",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }

    @Test
    fun `live list iterator methods work`() = testSession { terminal ->
        val nums = liveListOf(1, 2, 3, 4, 5, 6)
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
            textLine()

            text("List iterator (forward): ")
            with(nums.listIterator()) {
                while (hasNext()) {
                    text(next().toString())
                    if (hasNext()) {
                        text(", ")
                    }
                }
            }
            textLine()

            text("List iterator (reverse): ")
            with(nums.listIterator(nums.size)) {
                while (hasPrevious()) {
                    text(previous().toString())
                    if (hasPrevious()) {
                        text(", ")
                    }
                }
            }
            textLine()
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Iterator: 1, 2, 3, 4, 5, 6",
            "List iterator (forward): 1, 2, 3, 4, 5, 6",
            "List iterator (reverse): 6, 5, 4, 3, 2, 1",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }

    @Test
    fun `live list modify methods work`() = testSession { terminal ->
        val nums = liveListOf<Int>()
        section {
            text(nums.joinToString(", "))
        }.run {
            nums.withWriteLock {
                nums.add(123)
                nums.add(456)
                nums.add(789)
                nums.clear()

                nums.add(4)
                nums.add(6)
                nums.add(1, 5)
                nums.addAll(listOf(7, 8, 9))
                nums.addAll(0, listOf(-1, 2, 3))
                nums[0] = 1

                nums.add(123)
                nums.add(456)
                nums.add(789)
                nums.remove(123)
                nums.removeAll(listOf(456, 789))

                nums.add(123)
                nums.add(456)
                nums.add(789)
                nums.retainAll(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))

                nums.add(123)
                nums.removeAt(nums.indexOf(123))
            }
        }

        terminal.assertMatches {
            text("1, 2, 3, 4, 5, 6, 7, 8, 9")
        }
    }
}
