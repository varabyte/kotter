@file:Suppress("ConvertArgumentToSet")

package com.varabyte.kotter.foundation.collections

import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.terminal.inmemory.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
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
            "${Codes.Sgr.Reset}"
                    + "\r${Codes.Erase.CursorToLineEnd}${Codes.Cursor.MoveToPrevLine}".repeat(3)
                    + "\r${Codes.Erase.CursorToLineEnd}"
                    + "1 is spelled \"one\"",
            "2 is spelled \"two\"",
            "3 is spelled \"three\"",
            "4 is spelled \"four\"",
            "5 is spelled \"five\"",
            "6 is spelled \"six\"",
            "${Codes.Sgr.Reset}",
        ).inOrder()
    }

    @Test
    fun `live map query methods work`() = testSession { terminal ->
        fun squarePair(value: Int) = value to value * value
        val numSquares = liveMapOf((1..3).map(::squarePair))
        section {
            textLine("Set: [${numSquares.entries.joinToString(", ") { (num, squared) -> "$num² = $squared" }}]")
            textLine("Keys: [${numSquares.keys.joinToString(", ")}]")
            textLine("Values: [${numSquares.values.joinToString(", ")}]")
            textLine("Size: ${numSquares.size}")
            textLine("Is empty: ${numSquares.isEmpty()}")
            textLine("Contains key: ${numSquares.containsKey(4)}")
            textLine("Contains value: ${numSquares.containsValue(25)}")
            textLine("Square of 2: ${numSquares[2]}")
        }.run {
            numSquares.putAll((4..6).associate(::squarePair))
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
            "${Codes.Sgr.Reset}"
                    + "\r${Codes.Erase.CursorToLineEnd}${Codes.Cursor.MoveToPrevLine}".repeat(8)
                    + "\r${Codes.Erase.CursorToLineEnd}"
                    + "Set: [1² = 1, 2² = 4, 3² = 9, 4² = 16, 5² = 25, 6² = 36]",
            "Keys: [1, 2, 3, 4, 5, 6]",
            "Values: [1, 4, 9, 16, 25, 36]",
            "Size: 6",
            "Is empty: false",
            "Contains key: true",
            "Contains value: true",
            "Square of 2: 4",
            "${Codes.Sgr.Reset}",
        ).inOrder()
    }

    @Test
    fun `live map modify methods work`() = testSession { terminal ->
        val numSquares = liveMapOf<Int, Int>()
        section {
            text("Set: [${numSquares.entries.joinToString(", ") { (num, squared) -> "$num² = $squared" }}]")
        }.run {
            numSquares.withWriteLock {
                (1..10).forEach { i -> numSquares[i] = i }
                numSquares.clear()
                numSquares[0] = 123
                numSquares.remove(0)

                (1..3).forEach { i -> numSquares[i] = i * i }
                numSquares.putAll((4..6).associateWith { i -> i * i })
            }
        }

        terminal.assertMatches {
            text("Set: [1² = 1, 2² = 4, 3² = 9, 4² = 16, 5² = 25, 6² = 36]")
        }
    }

    private class TestMutableEntry<K, V>(override val key: K, initialValue: V) : MutableMap.MutableEntry<K, V> {
        constructor(keyValuePair: Pair<K, V>): this(keyValuePair.first, keyValuePair.second)

        override fun setValue(newValue: V): V {
            val prevValue = _value
            _value = newValue
            return prevValue
        }

        private var _value = initialValue
        override val value: V get() = _value

        override fun equals(other: Any?): Boolean {
            return other is TestMutableEntry<*, *> && this.key == other.key && this.value == other.value
        }

        override fun hashCode(): Int = arrayOf(key, value).contentHashCode()
    }

    @Test
    fun `live map entries property works`() = testSession { terminal ->
        val numSquares = liveMapOf<Int, Int>()
        val entries = numSquares.entries

        // Explicitly check `assertThat(entries.isEmpty())` for code coverage, not `assertThat(entries).isEmpty()`
        assertThat(entries.isEmpty()).isTrue()

        for (i in 0..10) {
            numSquares[i] = i * i
        }

        assertThat(entries).isNotEmpty()
        assertThat(entries.size).isEqualTo(11)

        assertThat(entries.contains(TestMutableEntry(5 to 5 * 5))).isTrue()
        assertThat(
            entries.containsAll(
            listOf(3 to 3 * 3, 6 to 6 * 6, 9 to 9 * 9).map { TestMutableEntry(it) }
        )).isTrue()

        // Kotlin MutableMap `entries` does not support addition!
        assertThrows<UnsupportedOperationException> {
            entries.add(TestMutableEntry(11 to 11 * 11))
        }
        assertThrows<UnsupportedOperationException> {
            entries.addAll(listOf(11 to 11 * 11, 12 to 12 * 12).map { TestMutableEntry(it) })
        }

        entries.remove(TestMutableEntry(0, 0))
        assertThat(entries.size).isEqualTo(10)
        assertThat(numSquares.size).isEqualTo(entries.size)

        entries.removeAll(entries.filter { it.key % 2 == 0 }) // Remove all evens
        assertThat(entries.size).isEqualTo(5)

        entries.retainAll(entries.filter { it.key <= 5 })
        assertThat(entries.size).isEqualTo(3)

        with(entries.iterator()) {
            while (hasNext()) {
                val next = next()
                if (next.key == 1) remove()
            }
        }

        assertThat(entries.map { it.key to it.value }).containsExactly(
            listOf(3 to 3 * 3, 5 to 5 * 5)
        ).inOrder()

        entries.clear()
        assertThat(entries).isEmpty()
        assertThat(numSquares).isEmpty()
    }

    @Test
    fun `live map keys property works`() = testSession { terminal ->
        val numSquares = liveMapOf<Int, Int>()
        val keys = numSquares.keys

        // Explicitly check `assertThat(keys.isEmpty())` for code coverage, not `assertThat(keys).isEmpty()`
        assertThat(keys.isEmpty()).isTrue()

        for (i in 0..10) {
            numSquares[i] = i * i
        }

        assertThat(keys).isNotEmpty()
        assertThat(keys.size).isEqualTo(11)

        assertThat(keys.contains(5)).isTrue()
        assertThat(keys.containsAll(listOf(3, 6, 9))).isTrue()

        // Kotlin MutableMap `keys` does not support addition!
        assertThrows<UnsupportedOperationException> {
            keys.add(11)
        }
        assertThrows<UnsupportedOperationException> {
            keys.addAll(listOf(11, 12))
        }

        keys.remove(0)
        assertThat(keys.size).isEqualTo(10)
        assertThat(numSquares.size).isEqualTo(keys.size)

        keys.removeAll((0..10).filter { it % 2 == 0 })
        assertThat(keys.size).isEqualTo(5)

        keys.retainAll((0 .. 10).filter { it <= 5 })
        assertThat(keys.size).isEqualTo(3)

        assertThat(keys).containsExactly(1, 3, 5).inOrder()

        keys.clear()

        assertThat(keys).isEmpty()
        assertThat(numSquares).isEmpty()
    }

    @Test
    fun `live map values property works`() = testSession { terminal ->
        val numSquares = liveMapOf<Int, Int>()
        val values = numSquares.values

        // Explicitly check `assertThat(values.isEmpty())` for code coverage, not `assertThat(values).isEmpty()`
        assertThat(values.isEmpty()).isTrue()

        for (i in 0..10) {
            numSquares[i] = i * i
        }

        assertThat(values).isNotEmpty()
        assertThat(values.size).isEqualTo(11)

        assertThat(values.contains(25)).isTrue()
        assertThat(values.containsAll(listOf(9, 36, 81))).isTrue()

        // Kotlin MutableMap `keys` does not support addition!
        assertThrows<UnsupportedOperationException> {
            values.add(100)
        }
        assertThrows<UnsupportedOperationException> {
            values.addAll(listOf(121, 144))
        }

        values.remove(0)
        assertThat(values.size).isEqualTo(10)
        assertThat(numSquares.size).isEqualTo(values.size)

        values.removeAll((0..10).map { it * it }.filter { it % 2 == 0 })
        assertThat(values.size).isEqualTo(5)

        values.retainAll((0 .. 10).map { it * it }.filter { it <= 25 })
        assertThat(values.size).isEqualTo(3)

        assertThat(values).containsExactly(1, 9, 25).inOrder()

        values.clear()

        assertThat(values).isEmpty()
        assertThat(numSquares).isEmpty()
    }
}
