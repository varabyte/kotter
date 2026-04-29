package com.varabyte.kotter.runtime.terminal

import com.varabyte.truthish.assertAll
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
    fun `grapheme utility methods work`() {
        val tm = TextMetrics()

        val graphemeRichText = "aiueo💩あいうえお\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66end"

        val expectedGraphemes = mutableListOf(
            "a" to 1,
            "i" to 1,
            "u" to 1,
            "e" to 1,
            "o" to 1,
            "💩" to 2,
            "あ" to 1,
            "い" to 1,
            "う" to 1,
            "え" to 1,
            "お" to 1,
            // The family emoji 👨‍👩‍👧‍👦 is a beast composed of 4 individual emoji (MAN, WOMAN, GIRL, BOY) glued together with
            // zero-width joiners (\u200D). We include it here to make sure the Kotter logic correctly finds the
            // beginning and doesn't stop at the last emoji in the list (i.e. BOY)
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66" to 11,
            "e" to 1,
            "n" to 1,
            "d" to 1,
        )

        var currIndex = graphemeRichText.length - 1
        while (currIndex >= 0) {
            val graphemeStart = tm.graphemeStartIndex(graphemeRichText, currIndex)
            val graphemeLen = tm.graphemeLengthAt(graphemeRichText, graphemeStart)
            val grapheme = graphemeRichText.substring(graphemeStart, graphemeStart + graphemeLen)

            val (expectedGrapheme, expectedGraphemeLen) = expectedGraphemes.removeLast()

            assertAll {
                that(grapheme).isEqualTo(expectedGrapheme)
                withMessage("Unexpected length of \"$grapheme\"").that(graphemeLen).isEqualTo(expectedGraphemeLen)
            }

            currIndex = graphemeStart - 1
        }
        assertThat(expectedGraphemes.isEmpty())

        // For good measure, make sure searching for the start index at every step of the family emoji ends up at the
        // beginning
        val familyEmoji = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"
        for (i in familyEmoji.indices) {
            assertThat(tm.graphemeStartIndex(familyEmoji, i)).isEqualTo(0)
        }
    }

    @Test
    fun `truncateToWidth works`() {
        val tm = TextMetrics()

        assertThrows<IllegalArgumentException>("Truncating Text with a newline did not result in an expected exception") {
            tm.truncateToWidth("Hello\nWorld", 1000)
        }

        run {
            val textEn = "Hello, World!"
            assertThat(tm.renderWidthOf(textEn)).isEqualTo(13)
            assertThat(tm.truncateToWidth(textEn, 4)).isEqualTo("Hell")
            assertThat(tm.truncateToWidth(textEn, 8)).isEqualTo("Hello, W")
            assertThat(tm.truncateToWidth(textEn, 13)).isEqualTo("Hello, World!")
            assertThat(tm.truncateToWidth(textEn, 9999)).isEqualTo("Hello, World!")

            assertThat(tm.truncateToWidth(textEn, 4, TruncateAt.START)).isEqualTo("rld!")
            assertThat(tm.truncateToWidth(textEn, 8, TruncateAt.START)).isEqualTo(", World!")
            assertThat(tm.truncateToWidth(textEn, 13, TruncateAt.START)).isEqualTo("Hello, World!")
            assertThat(tm.truncateToWidth(textEn, 9999, TruncateAt.START)).isEqualTo("Hello, World!")

            // Same checks with ellipsis
            assertThat(tm.truncateToWidth(textEn, 4, ellipsis = "...")).isEqualTo("H...")
            assertThat(tm.truncateToWidth(textEn, 8, ellipsis = "...")).isEqualTo("Hello...")
            assertThat(tm.truncateToWidth(textEn, 13, ellipsis = "...")).isEqualTo("Hello, World!")
            assertThat(tm.truncateToWidth(textEn, 9999, ellipsis = "...")).isEqualTo("Hello, World!")

            assertThat(tm.truncateToWidth(textEn, 4, TruncateAt.START, ellipsis = "...")).isEqualTo("...!")
            assertThat(tm.truncateToWidth(textEn, 8, TruncateAt.START, ellipsis = "...")).isEqualTo("...orld!")
            assertThat(tm.truncateToWidth(textEn, 13, TruncateAt.START, ellipsis = "...")).isEqualTo("Hello, World!")
            assertThat(tm.truncateToWidth(textEn, 9999, TruncateAt.START, ellipsis = "...")).isEqualTo("Hello, World!")

            assertThat(tm.truncateToWidth(textEn, 4, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("H...")
            assertThat(tm.truncateToWidth(textEn, 8, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("Hel...d!")
            assertThat(tm.truncateToWidth(textEn, 13, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("Hello, World!")
            assertThat(tm.truncateToWidth(textEn, 9999, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("Hello, World!")
        }

        run {
            val textEn = "Hello, World!"
            assertThat(tm.renderWidthOf(textEn)).isEqualTo(13)
            // Same checks with ellipsis
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

            assertThat(tm.truncateToWidth(textJa, 4, TruncateAt.START, ellipsis = "...")).isEqualTo("...!") // "*界!"
            assertThat(tm.truncateToWidth(textJa, 5, TruncateAt.START, ellipsis = "...")).isEqualTo("...!") // "世界!"
            assertThat(tm.truncateToWidth(textJa, 6, TruncateAt.START, ellipsis = "...")).isEqualTo("...界!") // " 世界!"
            assertThat(tm.truncateToWidth(textJa, 7, TruncateAt.START, ellipsis = "...")).isEqualTo("...界!") // ", 世界!"
            assertThat(tm.truncateToWidth(textJa, 8, TruncateAt.START, ellipsis = "...")).isEqualTo("...世界!") // "o, 世界!"
            assertThat(tm.truncateToWidth(textJa, 9, TruncateAt.START, ellipsis = "...")).isEqualTo("... 世界!") // "lo, 世界!"
            assertThat(tm.truncateToWidth(textJa, 10, TruncateAt.START, ellipsis = "...")).isEqualTo("..., 世界!") // "llo, 世界!"
            assertThat(tm.truncateToWidth(textJa, 11, TruncateAt.START, ellipsis = "...")).isEqualTo("...o, 世界!") // "ello, 世界!"
            assertThat(tm.truncateToWidth(textJa, 12, TruncateAt.START, ellipsis = "...")).isEqualTo("Hello, 世界!") // "Hello, 世界!"

            assertThat(tm.truncateToWidth(textJa, 4, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("H...") // "He" "*!"
            assertThat(tm.truncateToWidth(textJa, 5, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("H...!") // "Hel" "*!"
            assertThat(tm.truncateToWidth(textJa, 6, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("He...!") // "Hel" "界!"
            assertThat(tm.truncateToWidth(textJa, 7, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("He...!") // "Hell" "界!"
            assertThat(tm.truncateToWidth(textJa, 8, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("He...界!") // "Hell" "*界!"
            assertThat(tm.truncateToWidth(textJa, 9, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("Hel...界!") // "Hello" "*界!"
            assertThat(tm.truncateToWidth(textJa, 10, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("Hel...界!") // "Hello" "世界!"
            assertThat(tm.truncateToWidth(textJa, 11, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("Hel...世界!") // "Hello," "世界!"
            assertThat(tm.truncateToWidth(textJa, 12, TruncateAt.MIDDLE, ellipsis = "...")).isEqualTo("Hello, 世界!") // "Hello, 世界!"
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
