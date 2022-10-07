package com.varabyte.kotter.runtime.internal.text

import com.varabyte.truthish.assertThat
import kotlin.test.Test

class TextPtrTest {
    @Test
    fun `can set text ptr index`() {
        val textPtr = TextPtr("Hello")

        assertThat(textPtr.currChar).isEqualTo('H')

        textPtr.charIndex = 1
        assertThat(textPtr.currChar).isEqualTo('e')

        textPtr.charIndex = 4
        assertThat(textPtr.currChar).isEqualTo('o')
    }

    @Test
    fun `can increment through text`() {
        val textPtr = TextPtr("Hello")

        assertThat(textPtr.currChar).isEqualTo('H')
        assertThat(textPtr.charIndex).isEqualTo(0)
        assertThat(textPtr.remainingLength).isEqualTo(5)

        assertThat(textPtr.increment()).isTrue()
        assertThat(textPtr.currChar).isEqualTo('e')
        assertThat(textPtr.charIndex).isEqualTo(1)
        assertThat(textPtr.remainingLength).isEqualTo(4)

        assertThat(textPtr.increment()).isTrue()
        assertThat(textPtr.currChar).isEqualTo('l')
        assertThat(textPtr.charIndex).isEqualTo(2)
        assertThat(textPtr.remainingLength).isEqualTo(3)

        assertThat(textPtr.increment()).isTrue()
        assertThat(textPtr.currChar).isEqualTo('l')
        assertThat(textPtr.charIndex).isEqualTo(3)
        assertThat(textPtr.remainingLength).isEqualTo(2)

        assertThat(textPtr.increment()).isTrue()
        assertThat(textPtr.currChar).isEqualTo('o')
        assertThat(textPtr.charIndex).isEqualTo(4)
        assertThat(textPtr.remainingLength).isEqualTo(1)

        // You can increment one past the last character
        assertThat(textPtr.increment()).isTrue()
        assertThat(textPtr.currChar).isEqualTo(Char.MIN_VALUE)
        assertThat(textPtr.charIndex).isEqualTo(5)
        assertThat(textPtr.remainingLength).isEqualTo(0)

        assertThat(textPtr.increment()).isFalse()
        assertThat(textPtr.charIndex).isEqualTo(5)
    }

    @Test
    fun `can decrement through text`() {
        val textPtr = TextPtr("Hello", 5)

        assertThat(textPtr.currChar).isEqualTo(Char.MIN_VALUE)
        assertThat(textPtr.charIndex).isEqualTo(5)
        assertThat(textPtr.remainingLength).isEqualTo(0)

        assertThat(textPtr.decrement()).isTrue()
        assertThat(textPtr.currChar).isEqualTo('o')
        assertThat(textPtr.charIndex).isEqualTo(4)
        assertThat(textPtr.remainingLength).isEqualTo(1)

        assertThat(textPtr.decrement()).isTrue()
        assertThat(textPtr.currChar).isEqualTo('l')
        assertThat(textPtr.charIndex).isEqualTo(3)
        assertThat(textPtr.remainingLength).isEqualTo(2)

        assertThat(textPtr.decrement()).isTrue()
        assertThat(textPtr.currChar).isEqualTo('l')
        assertThat(textPtr.charIndex).isEqualTo(2)
        assertThat(textPtr.remainingLength).isEqualTo(3)

        assertThat(textPtr.decrement()).isTrue()
        assertThat(textPtr.currChar).isEqualTo('e')
        assertThat(textPtr.charIndex).isEqualTo(1)
        assertThat(textPtr.remainingLength).isEqualTo(4)

        assertThat(textPtr.decrement()).isTrue()
        assertThat(textPtr.currChar).isEqualTo('H')
        assertThat(textPtr.charIndex).isEqualTo(0)
        assertThat(textPtr.remainingLength).isEqualTo(5)

        assertThat(textPtr.decrement()).isFalse()
        assertThat(textPtr.charIndex).isEqualTo(0)
    }

    @Test
    fun `can increment while`() {
        val textPtr = TextPtr("ABCDE")

        assertThat(textPtr.currChar).isEqualTo('A')

        assertThat(textPtr.incrementWhile { it != 'C' }).isTrue()
        assertThat(textPtr.currChar).isEqualTo('C')

        assertThat(textPtr.incrementWhile { it != 'X' }).isTrue()
        assertThat(textPtr.currChar).isEqualTo(Char.MIN_VALUE)

        assertThat(textPtr.incrementWhile { it != 'X' }).isFalse()
    }

    @Test
    fun `can decrement while`() {
        val textPtr = TextPtr("ABCDE", 4)

        assertThat(textPtr.currChar).isEqualTo('E')

        assertThat(textPtr.decrementWhile { it != 'B' }).isTrue()
        assertThat(textPtr.currChar).isEqualTo('B')

        assertThat(textPtr.decrementWhile { it != 'X' }).isTrue()
        assertThat(textPtr.currChar).isEqualTo('A')

        assertThat(textPtr.decrementWhile { it != 'X' }).isFalse()
    }

    @Test
    fun `can increment until`() {
        val textPtr = TextPtr("ABCDE")

        assertThat(textPtr.currChar).isEqualTo('A')

        assertThat(textPtr.incrementUntil { it == 'C' }).isTrue()
        assertThat(textPtr.currChar).isEqualTo('C')

        assertThat(textPtr.incrementUntil { it == 'X' }).isTrue()
        assertThat(textPtr.currChar).isEqualTo(Char.MIN_VALUE)

        assertThat(textPtr.incrementUntil { it == 'X' }).isFalse()
    }

    @Test
    fun `can decrement until`() {
        val textPtr = TextPtr("ABCDE", 4)

        assertThat(textPtr.currChar).isEqualTo('E')

        assertThat(textPtr.decrementUntil { it == 'B' }).isTrue()
        assertThat(textPtr.currChar).isEqualTo('B')

        assertThat(textPtr.decrementUntil { it == 'X' }).isTrue()
        assertThat(textPtr.currChar).isEqualTo('A')

        assertThat(textPtr.decrementUntil { it == 'X' }).isFalse()
    }

    @Test
    fun `can create substring`() {
        val textPtr = TextPtr("Debugging")

        assertThat(textPtr.substring(0)).isEqualTo("")
        assertThat(textPtr.substring(5)).isEqualTo("Debug")
        assertThat(textPtr.substring(Int.MAX_VALUE)).isEqualTo("Debugging")

        textPtr.charIndex = 2
        assertThat(textPtr.substring(0)).isEqualTo("")
        assertThat(textPtr.substring(3)).isEqualTo("bug")
        assertThat(textPtr.substring(Int.MAX_VALUE)).isEqualTo("bugging")

        textPtr.charIndex = textPtr.text.length
        assertThat(textPtr.substring(0)).isEqualTo("")
        assertThat(textPtr.substring(Int.MAX_VALUE)).isEqualTo("")
    }

    @Test
    fun `can parse int values`() {
        val text = "Hello123 456 789goodbye"
        val textPtr = TextPtr(text)

        assertThat(textPtr.readInt()).isNull()

        textPtr.charIndex = text.indexOf("1")
        assertThat(textPtr.readInt()).isEqualTo(123)

        textPtr.charIndex = text.indexOf("2")
        assertThat(textPtr.readInt()).isEqualTo(23)

        textPtr.charIndex = text.indexOf("4")
        assertThat(textPtr.readInt()).isEqualTo(456)

        textPtr.charIndex = text.indexOf("7")
        assertThat(textPtr.readInt()).isEqualTo(789)

        textPtr.charIndex = text.length
        assertThat(textPtr.readInt()).isNull()
    }
}