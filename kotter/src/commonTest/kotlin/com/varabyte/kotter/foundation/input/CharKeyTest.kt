package com.varabyte.kotter.foundation.input

import com.varabyte.truthish.assertThat
import kotlin.test.Test

class CharKeyTest {
    @Test
    fun charKeysAreCaseInsensitive() {
        val lowerA1 = CharKey('a')
        val lowerA2 = CharKey('a')
        val upperA1 = CharKey('A')
        val upperA2 = CharKey('A')
        val lowerZ = CharKey('z')
        val upperZ = CharKey('Z')

        assertThat(lowerA1).isEqualTo(lowerA2)
        assertThat(lowerA1).isEqualTo(upperA1)
        assertThat(lowerA2).isEqualTo(upperA2)
        assertThat(lowerA1.char).isEqualTo(lowerA2.char)
        assertThat(lowerA1.char).isNotEqualTo(upperA1.char)

        assertThat(lowerA1).isNotEqualTo(lowerZ)
        assertThat(upperA1).isNotEqualTo(upperZ)

        assertThat(lowerA1.hashCode()).isEqualTo(lowerA2.hashCode())
        assertThat(lowerA1.hashCode()).isEqualTo(upperA1.hashCode())
    }

    @Test
    fun upperAndLowerConvenienceMethodsWorkAsExpected() {
        val lowerA1 = CharKey('a')
        val lowerA2 = CharKey('a').lower()
        val lowerA3 = CharKey('A').lower()

        val upperA1 = CharKey('A')
        val upperA2 = CharKey('A').upper()
        val upperA3 = CharKey('a').upper()

        assertThat(lowerA1.char).isEqualTo(lowerA2.char)
        assertThat(lowerA1.char).isEqualTo(lowerA3.char)
        assertThat(upperA1.char).isEqualTo(upperA2.char)
        assertThat(upperA1.char).isEqualTo(upperA3.char)

        assertThat(lowerA1.isLower()).isTrue()
        assertThat(lowerA1.isUpper()).isFalse()
        assertThat(upperA1.isLower()).isFalse()
        assertThat(upperA1.isUpper()).isTrue()
    }
}
