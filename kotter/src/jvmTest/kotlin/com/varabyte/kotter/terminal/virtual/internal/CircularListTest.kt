package com.varabyte.kotter.terminal.virtual.internal

import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import org.junit.Test

class CircularListTest {
    @Test
    fun `can create simple buffer`() {
        assertThrows<IllegalArgumentException> { CircularList<Int>(initialSize = CircularList.MINIMUM_SIZE - 1) } // initial size must be > 2
        val buffer = CircularList<Int>(CircularList.MINIMUM_SIZE) // Low initial size to ensure auto-grow works
        assertThat(buffer).isEmpty()

        // Can add at end
        for (i in 0..5) buffer.add(i * 2)
        assertThat(buffer).containsAllIn(0, 2, 4, 6, 8, 10).inOrder()

        // Can insert in the middle
        for (i in 1 .. 9 step 2) {
            buffer.add(i, i)
        }
        assertThat(buffer).containsAllIn(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).inOrder()

        // Can overwrite values
        for (i in 0..10 step 2) {
            buffer[i] = 100 + i
        }
        assertThat(buffer).containsAllIn(100, 1, 102, 3, 104, 5, 106, 7, 108, 9, 110).inOrder()

        // Can use calls that rely internally on iteration
        assertThat(buffer.filter { it % 2 == 1 }).containsAllIn(1, 3, 5, 7, 9).inOrder()

        buffer.clear()
        assertThat(buffer).isEmpty()
    }

    @Test
    fun `can add to and remove from both sides`() {
        // Large initial size to avoid growing the buffer while adding elements; this makes sure our start and end
        // indices don't get renormalized
        val buffer = CircularList<Int>(100)

        for (i in 0..5) buffer.addFirst(i * 2)
        assertThat(buffer).containsAllIn(10, 8, 6, 4, 2, 0).inOrder()

        for (i in 6..10) buffer.add(i * 2)
        assertThat(buffer).containsAllIn(10, 8, 6, 4, 2, 0, 12, 14, 16, 18, 20).inOrder()

        assertThat(buffer.removeFirst()).isEqualTo(10)
        assertThat(buffer.removeLast()).isEqualTo(20)
        assertThat(buffer.removeFirst()).isEqualTo(8)
        assertThat(buffer.removeLast()).isEqualTo(18)
        assertThat(buffer.removeFirst()).isEqualTo(6)
        assertThat(buffer.removeLast()).isEqualTo(16)
        assertThat(buffer.removeFirst()).isEqualTo(4)
        assertThat(buffer.removeLast()).isEqualTo(14)
        assertThat(buffer.removeFirst()).isEqualTo(2)
        assertThat(buffer.removeLast()).isEqualTo(12)
        assertThat(buffer.removeFirst()).isEqualTo(0)

        assertThrows<IndexOutOfBoundsException> { buffer.removeFirst() }
        assertThrows<IndexOutOfBoundsException> { buffer.removeLast() }
    }

    @Test
    fun `ensure we get internal start and end indexes flipped`() {
        assertThrows<IllegalArgumentException> { CircularList<Int>(initialSize = 0) }
        val buffer = CircularList<Int>()
        assertThat(buffer.size).isEqualTo(0)
        for (i in 0..5) buffer.add(i * 2)
        assertThat(buffer.size).isEqualTo(6)

        assertThat(buffer).containsAllIn(0, 2, 4, 6, 8, 10).inOrder()
    }

}

