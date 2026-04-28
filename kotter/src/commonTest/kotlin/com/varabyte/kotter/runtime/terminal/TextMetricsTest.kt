package com.varabyte.kotter.runtime.terminal

import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlin.test.Test

class TextMetricsTest {
    @Test
    fun `renderWidthOf char works`() {
        val tm = TextMetrics()

        assertThat(tm.renderWidthOf('a')).isEqualTo(1)
        assertThat(tm.renderWidthOf('何')).isEqualTo(2) // Double width
    }

    @Test
    fun `renderWidthOf string works`() {
        val tm = TextMetrics()

        assertThat(tm.renderWidthOf("Hello")).isEqualTo(5)
        assertThat(tm.renderWidthOf("💩")).isEqualTo(2)
    }

    @Test
    fun `truncateToWidth works`() {
        val tm = TextMetrics()

        run {
            val textEn = "Hello, World!"
            assertThat(tm.renderWidthOf(textEn)).isEqualTo(13)
            assertThat(tm.truncateToWidth(textEn, 4)).isEqualTo("Hell")
            assertThat(tm.truncateToWidth(textEn, 8)).isEqualTo("Hello, W")
            assertThat(tm.truncateToWidth(textEn, 9999)).isEqualTo("Hello, World!")

            // Same checks with ellipsis
            assertThat(tm.truncateToWidth(textEn, 4, ellipsis = "...")).isEqualTo("H...")
            assertThat(tm.truncateToWidth(textEn, 8, ellipsis = "...")).isEqualTo("Hello...")
            assertThat(tm.truncateToWidth(textEn, 9999, ellipsis = "...")).isEqualTo("Hello, World!")
        }

        assertThrows<IllegalArgumentException>("Truncating text with a newline did not result in an expected exception") {
            tm.truncateToWidth("Hello\nWorld", 1000)
        }

        run {
            val textJa = "Hello, 世界!"
            assertThat(tm.renderWidthOf(textJa)).isEqualTo(12)
            assertThat(tm.renderWidthOf('世')).isEqualTo(2)
            assertThat(tm.renderWidthOf('界')).isEqualTo(2)

            assertThat(tm.truncateToWidth(textJa, 4)).isEqualTo("Hell")
            assertThat(tm.truncateToWidth(textJa, 5)).isEqualTo("Hello")
            assertThat(tm.truncateToWidth(textJa, 6)).isEqualTo("Hello,")
            assertThat(tm.truncateToWidth(textJa, 7)).isEqualTo("Hello, ")
            assertThat(tm.truncateToWidth(textJa, 8)).isEqualTo("Hello, ") // Can't quite fit 世
            assertThat(tm.truncateToWidth(textJa, 9)).isEqualTo("Hello, 世")
            assertThat(tm.truncateToWidth(textJa, 10)).isEqualTo("Hello, 世") // Can't quite fit 界
            assertThat(tm.truncateToWidth(textJa, 11)).isEqualTo("Hello, 世界")
            assertThat(tm.truncateToWidth(textJa, 12)).isEqualTo("Hello, 世界!")

            // Same checks with ellipsis
            // (*) in the comments below means we want to render a double width character but can't
            assertThat(tm.truncateToWidth(textJa, 4, ellipsis = "...")).isEqualTo("H...") // "Hell"
            assertThat(tm.truncateToWidth(textJa, 5, ellipsis = "...")).isEqualTo("He...") // "Hello"
            assertThat(tm.truncateToWidth(textJa, 6, ellipsis = "...")).isEqualTo("Hel...") // "Hello,"
            assertThat(tm.truncateToWidth(textJa, 7, ellipsis = "...")).isEqualTo("Hell...") // "Hello, "
            assertThat(tm.truncateToWidth(textJa, 8, ellipsis = "...")).isEqualTo("Hello...") // "Hello, *"
            assertThat(tm.truncateToWidth(textJa, 9, ellipsis = "...")).isEqualTo("Hello,...") // "Hello, 世"
            assertThat(tm.truncateToWidth(textJa, 10, ellipsis = "...")).isEqualTo("Hello, ...") // "Hello, 世*"
            assertThat(tm.truncateToWidth(textJa, 11, ellipsis = "...")).isEqualTo("Hello, ...") // "Hello, 世界"
            assertThat(tm.truncateToWidth(textJa, 12, ellipsis = "...")).isEqualTo("Hello, 世界!") // "Hello, 世界!"
        }

        run {
            // Test really small widths where the ellipsis doesn't fit
            val dummyText = "This is all going to get clipped anyway"
            assertThat(tm.truncateToWidth(dummyText, 3, ellipsis = "...")).isEqualTo("...")
            assertThat(tm.truncateToWidth(dummyText, 2, ellipsis = "...")).isEqualTo("..")
            assertThat(tm.truncateToWidth(dummyText, 1, ellipsis = "...")).isEqualTo(".")
        }

        run {
            // Misc edge cases
            assertThat(tm.truncateToWidth("", 1000)).isEqualTo("")
            assertThat(tm.truncateToWidth("xyz", 0)).isEqualTo("")
            assertThat(tm.truncateToWidth("xyz", 0, ellipsis = "...")).isEqualTo("")
        }

    }
}
