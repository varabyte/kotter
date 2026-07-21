package com.varabyte.kotter.terminal.virtual.internal

import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import org.junit.Test
import java.awt.Color

class DocumentStylesTest {
    @Test
    fun `can create and populate document styles`() {
        val docStyles = MutableDocumentStyles(defaultFgColor = Color.WHITE, defaultBgColor = Color.BLACK)

        val textStyle = docStyles.createEmptyTextStyle()

        assertThrows<IllegalArgumentException> {
            docStyles.put(-1, textStyle)
        }

        textStyle.fgColor = Color.MAGENTA
        textStyle.isBold = true
        docStyles.put(2, textStyle)

        textStyle.isUnderline = true
        docStyles.put(8, textStyle)

        textStyle.fgColor = Color.ORANGE
        docStyles.put(10, textStyle)

        textStyle.clear()
        docStyles.put(15, textStyle)

        textStyle.isStrikethrough = true
        docStyles.put(18, textStyle)

        val expectedAt0 = docStyles.createEmptyTextStyle()
        for (i in 0 until 2) {
            assertThat(docStyles.at(i)).isEqualTo(expectedAt0)
        }

        val expectedAt2 = expectedAt0.copy(fgColor = Color.MAGENTA, isBold = true)
        for (i in 2 until 8) {
            assertThat(docStyles.at(i)).isEqualTo(expectedAt2)
        }

        val expectedAt8 = expectedAt2.copy(isUnderlined = true)
        for (i in 8 until 10) {
            assertThat(docStyles.at(i)).isEqualTo(expectedAt8)
        }

        val expectedAt10 = expectedAt8.copy(fgColor = Color.ORANGE)
        for (i in 10 until 15) {
            assertThat(docStyles.at(i)).isEqualTo(expectedAt10)
        }

        val expectedAt15 = docStyles.createEmptyTextStyle() // cleared
        for (i in 15 until 18) {
            assertThat(docStyles.at(i)).isEqualTo(expectedAt15)
        }

        val expectedAt18 = expectedAt15.copy(isStrikethrough = true)
        for (i in 18 until 20) {
            // There is no "end index" for styles. Anything past the last entry just has the last entry's value.
            assertThat(docStyles.at(i)).isEqualTo(expectedAt18)
        }
    }

    @Test
    fun `inserting styles at older indices preserves existing state`() {
        val docStyles = MutableDocumentStyles(defaultFgColor = Color.WHITE, defaultBgColor = Color.BLACK)
        fun DocumentStyles.createColorString(length: Int): String {
            val self = this
            return buildString {
                for (i in 0 until length) {
                    append(
                        when (self.at(i).fgColor) {
                            Color.RED -> 'r'
                            Color.GREEN -> 'g'
                            Color.BLUE -> 'b'
                            else -> '?'
                        }
                    )
                }
            }
        }

        docStyles.put(0) { fgColor = Color.RED }
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrrrrrrrrrrr")

        docStyles.put(10) { fgColor = Color.GREEN }
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrrrrrrggggg")

        // Add an arbitrary endpoint, so that when we later have a blue style start stepping in green territory, we'll
        // know to push green back rather than just overwrite it
        docStyles.put(100) { fgColor = Color.MAGENTA }
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrrrrrrggggg")

        val blueStyle = docStyles.put(5) { fgColor = Color.BLUE }
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrbrrrrggggg")

        docStyles.put(6, blueStyle)
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrbbrrrggggg")

        docStyles.put(7, blueStyle)
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrbbbrrggggg")

        docStyles.put(8, blueStyle)
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrbbbbrggggg")

        docStyles.put(9, blueStyle)
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrbbbbbggggg")

        docStyles.put(10, blueStyle)
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrbbbbbbgggg")

        docStyles.put(11, blueStyle)
        assertThat(docStyles.createColorString(length = 15)).isEqualTo("rrrrrbbbbbbbggg")

        // For completion, do one more quick test where we add the same style in reverse right before itself to ensure
        // that it at least works fine, but using the debugger, we can also check that the algorithm does the smart
        // thing and removes redundant intermediate nodes
        docStyles.clear()
        docStyles.put(0) { fgColor = Color.RED }
        val greenStyle = docStyles.put(10) { fgColor = Color.GREEN }
        assertThat(docStyles.createColorString(15)).isEqualTo("rrrrrrrrrrggggg")

        for (i in 8 downTo 5) {
            docStyles.put(i, greenStyle)
        }
        // At this point, we should have r@0, g@5, r@9, g@10
        assertThat(docStyles.createColorString(15)).isEqualTo("rrrrrggggrggggg")

        // At this point, we should have r@0, g@5
        docStyles.put(9, greenStyle)
        assertThat(docStyles.createColorString(15)).isEqualTo("rrrrrgggggggggg")
    }

    @Test
    fun `can remove elements`() {
        val docStyles = MutableDocumentStyles(defaultFgColor = Color.WHITE, defaultBgColor = Color.BLACK)

        val roygbiv =
            listOf(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA)
        roygbiv.forEachIndexed { index, color ->
            docStyles.put(index) { fgColor = color }
        }

        roygbiv.forEachIndexed { index, color ->
            assertThat(docStyles.at(index).fgColor).isEqualTo(color)
        }

        docStyles.removeRange(roygbiv.indexOf(Color.ORANGE) .. roygbiv.indexOf(Color.BLUE))

        val expected = listOf(Color.RED, Color.MAGENTA)
        expected.forEachIndexed { index, color ->
            assertThat(docStyles.at(index).fgColor).isEqualTo(color)
        }

        docStyles.clear()
        run {
            assertThat(docStyles.at(0).fgColor).isEqualTo(docStyles.defaultFgColor)
            // There is no "end index" for styles. Anything past the last entry just has the last entry's value.
            assertThat(docStyles.at(Int.MAX_VALUE).fgColor).isEqualTo(docStyles.defaultFgColor)
        }
    }

    @Test
    fun `removing ranges still preserves previous styles`() {
        val docStyles = MutableDocumentStyles(defaultFgColor = Color.WHITE, defaultBgColor = Color.BLACK)
        fun DocumentStyles.createColorString(length: Int): String {
            val self = this
            return buildString {
                for (i in 0 until length) {
                    append(
                        when (self.at(i).fgColor) {
                            Color.RED -> 'r'
                            Color.GREEN -> 'g'
                            Color.BLUE -> 'b'
                            Color.ORANGE -> 'o'
                            Color.MAGENTA -> 'm'
                            else -> '?'
                        }
                    )
                }
            }
        }

        docStyles.put(0) { fgColor = Color.RED }
        docStyles.put(3) { fgColor = Color.GREEN }
        docStyles.put(6) { fgColor = Color.ORANGE }
        docStyles.put(9) { fgColor = Color.MAGENTA }
        docStyles.put(12) { fgColor = Color.BLUE }

        assertThat(docStyles.createColorString(15)).isEqualTo("rrrgggooommmbbb")

        // Delete multiple style indices -- the last one in the group should get preserved
        docStyles.removeRange(2..10)
        assertThat(docStyles.createColorString(6)).isEqualTo("rrmbbb")

        // Make sure that when we delete a value, we don't push it over an existing style that is already set (here,
        // magenta shouldn't get pushed over into the territory already claimed by blue)
        docStyles.removeAt(2)
        assertThat(docStyles.createColorString(5)).isEqualTo("rrbbb")

        docStyles.removeFrom(3)
        assertThat(docStyles.createColorString(3)).isEqualTo("rrb")
    }
}

