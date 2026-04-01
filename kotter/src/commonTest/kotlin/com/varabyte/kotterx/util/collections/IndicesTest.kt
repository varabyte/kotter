package com.varabyte.kotterx.util.collections

import com.varabyte.truthish.assertThat
import kotlin.test.Test

class IndicesTest {
    @Test
    fun `indices can resolve and contain expected values`() {
        val indices = indicesOf {
            add(1)
            addRange(3,5)
            add(7)
            addRange(-5, -3)
            add(-1)
        }

        indices.resolve(maxIndex = 19).let { resolvedSize20 ->
            // Test absolute indices / ranges
            assertThat(resolvedSize20.contains(0)).isFalse()
            assertThat(resolvedSize20.contains(1)).isTrue()
            assertThat(resolvedSize20.contains(2)).isFalse()

            assertThat(resolvedSize20.contains(4)).isTrue()

            assertThat(resolvedSize20.contains(6)).isFalse()
            assertThat(resolvedSize20.contains(7)).isTrue()
            assertThat(resolvedSize20.contains(8)).isFalse()

            // Test relative indices / ranges
            // -1 is 19, -3 is 17, and -5 is 15

            assertThat(resolvedSize20.contains(14)).isFalse()
            assertThat(resolvedSize20.contains(15)).isTrue()
            assertThat(resolvedSize20.contains(16)).isTrue()
            assertThat(resolvedSize20.contains(17)).isTrue()
            assertThat(resolvedSize20.contains(18)).isFalse()
            assertThat(resolvedSize20.contains(19)).isTrue()

            assertThat(resolvedSize20.contains(20)).isFalse()
        }

        indices.resolve(maxIndex = 99).let { resolvedSize100 ->
            // Test absolute indices / ranges

            assertThat(resolvedSize100.contains(0)).isFalse()
            assertThat(resolvedSize100.contains(1)).isTrue()
            assertThat(resolvedSize100.contains(2)).isFalse()

            assertThat(resolvedSize100.contains(4)).isTrue()

            assertThat(resolvedSize100.contains(6)).isFalse()
            assertThat(resolvedSize100.contains(7)).isTrue()
            assertThat(resolvedSize100.contains(8)).isFalse()

            // Test relative indices / ranges

            // First, make sure what asserted true in the size 20 case does't assert true now

            assertThat(resolvedSize100.contains(15)).isFalse()
            assertThat(resolvedSize100.contains(16)).isFalse()
            assertThat(resolvedSize100.contains(17)).isFalse()
            assertThat(resolvedSize100.contains(19)).isFalse()

            // -1 is 99, -3 is 97, and -5 is 95

            assertThat(resolvedSize100.contains(94)).isFalse()
            assertThat(resolvedSize100.contains(95)).isTrue()
            assertThat(resolvedSize100.contains(96)).isTrue()
            assertThat(resolvedSize100.contains(97)).isTrue()
            assertThat(resolvedSize100.contains(98)).isFalse()
            assertThat(resolvedSize100.contains(99)).isTrue()

            assertThat(resolvedSize100.contains(20)).isFalse()
        }
    }

    @Test
    fun `indices out of range are ignored`() {
        val indices = indicesOf {
            add(9999)
            addRange(0, 100)
            addRange(-9999, -1)
        }.resolve(maxIndex = 1)

        assertThat(indices.contains(0)).isTrue()
        assertThat(indices.contains(1)).isTrue()

        assertThat(indices.contains(9999)).isFalse()
        assertThat(indices.contains(2)).isFalse()
        assertThat(indices.contains(-1)).isFalse()
    }

    @Test
    fun `invalid ranges are ignored`() {
        val indices = indicesOf {
            addRange(10, 0)
            addRange(-1, -11) // resolved as 10, 0
        }.resolve(maxIndex = 10)

        for (i in 0..10) {
            assertThat(indices.contains(i)).isFalse()
        }
    }

    @Test
    fun `duplicate values are fine`() {
        val indices = indicesOf {
            add(1)
            add(1)
            add(1)
            addRange(3,7)
            addRange(4,9)
            addRange(-3, -1)
        }.resolve(maxIndex = 10)

        assertThat(indices.contains(0)).isFalse()
        assertThat(indices.contains(1)).isTrue()
        assertThat(indices.contains(2)).isFalse()
        assertThat(indices.contains(3)).isTrue()
        assertThat(indices.contains(4)).isTrue()
        assertThat(indices.contains(5)).isTrue()
        assertThat(indices.contains(6)).isTrue()
        assertThat(indices.contains(7)).isTrue()
        assertThat(indices.contains(8)).isTrue()
        assertThat(indices.contains(9)).isTrue()
        assertThat(indices.contains(10)).isTrue()

    }
}
