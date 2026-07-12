package com.varabyte.kotter.terminal.virtual.internal

import java.awt.Color
import java.util.TreeMap

/**
 * A collection of [TextStyle] instances that should get applied across an entire document.
 */
internal interface DocumentStyles {
    val defaultFgColor: Color
    val defaultBgColor: Color

    /**
     * Return a new [TextStyle] instance filled with styles at the specified index.
     *
     * This style is yours to use.
     */
    fun at(index: Int): TextStyle
}

/**
 * Create an empty [TextStyle] instance.
 *
 * This text style is NOT registered with the document; it is just a new, empty instance for the caller's use,
 * configured with defaults provided by this class.
 *
 * It is a useful pattern to create this and then set it with the results from [at].
 */
internal fun DocumentStyles.createEmptyTextStyle() = MutableTextStyle(defaultFgColor, defaultBgColor)

/**
 * A mutable [DocumentStyles] where you can add new styles to it.
 *
 * This class uses efficient algorithms internally, but you are expected to add a style per character in your document.
 * We will ensure that we don't waste time saving redundant style values.
 *
 * This does mean that if you add previous styles and then go back and insert new styles at an older index, you should
 * expect that all other indices hold onto their previous states, as if they were already baked in.
 *
 * If you skip some indices, then the style from the last valid index matters.
 */
internal class MutableDocumentStyles(override val defaultFgColor: Color, override val defaultBgColor: Color) :
    DocumentStyles {
    private val emptyStyle: TextStyle = createEmptyTextStyle()
    private val styles = TreeMap<Int, TextStyle>()

    private val lastIndex get() = if (styles.isNotEmpty()) styles.lastKey() else -1

    private fun TreeMap<Int, TextStyle>.atOrBefore(index: Int): TextStyle {
        check(index >= 0)
        // We know there will ALWAYS be a floor entry because we always have at least an entry at index 0
        return this.floorEntry(index)?.value ?: emptyStyle
    }

    private fun TreeMap<Int, TextStyle>.rangeSequence(range: IntRange): Sequence<Pair<Int, TextStyle>> {
        val map = this
        return sequence {
            var i: Int? = range.first
            while (true) {
                val entry = map.ceilingEntry(i)
                if (entry == null || entry.key > range.last) break
                yield(entry.key to entry.value)
                i = entry.key + 1
            }
        }
    }

    /**
     * Set a style at the specified index.
     *
     * Note that the order of added styles matters! Once you've added styles at later indices, if you go back to an
     * earlier index and add a style there, the state of this class will be updated in a way that preserves the previous
     * state.
     *
     * For example, if you have red @ index 0 and green @ 10, and then you go back to 5 and put blue there, it will do
     * that but 6 will be set to red. You will have to keep putting more blue values in if you want to overwrite more of
     * them. (Internally, we do not store redundant entries!)
     *
     * If your index is set to the very last index already entered, it will not be preserved but will be overwritten.
     * Only if you set a style to an index BEFORE the last one will there be attempts to preservere old state.
     *
     * To bring this all together, let's say we have a red style at 0 and a green style at 10. You can think of the
     * styles as looking like this "rrrrrrrrrggg..." (and green forever).
     *
     * If you set blue at index 5, then you get "rrrrrbrrrrggg..."
     *
     * If you then set blue to index 6 through index 10, it will be "rrrrrbbbbbbbb..." (and blue forever).
     *
     * This seems potentially confusing, because we allow indices to be specified sparsely, but in practice, if you
     * meticulously set every index yourself, it is a much easier mental model to think about. In other words, don't
     * think: "red @ 0, green @ 10, then blue @ 5", but think "red @ 0..9, green @ 10..15, then blue @ 5..10"
     */
    fun put(index: Int, style: TextStyle) {
        require(index >= 0) { "Attempting to add attribute at invalid index $index"}

        // Protect ourselves from callers passing in mutable styles and changing them later
        fun styleCopy() = style.copy()

        // If here, we are inserting a style in front of other ones, which means we should think of this code as
        // overwriting existing styles.
        val currStyle = styles.atOrBefore(index)

        // Adding to the end should always be trivial
        if (index >= lastIndex) {
            if (style != currStyle) {
                styles[index] = styleCopy()
            }
            return
        }

        // In order to preserve the previous state of styles, we don't overwrite styles but just push them out of the
        // way to the next index (unless that index is already set). The 0th index is special
        if (style != currStyle) {
            styles.remove(index)
            if (styles[index + 1] == null) styles[index + 1] = currStyle
        }

        // It's not expected, but a user might have added an identical style directly before another one, at which
        // point, we can remove the following one, as it is now redundant.
        styles.ceilingKey(index + 1)?.let { nextStyleIndex ->
            val nextStyle = styles.getValue(nextStyleIndex)
            if (nextStyle == style) styles.remove(nextStyleIndex)
        }

        // Adding the same style as before would be redundant, so if that's the case, we're done!
        val prevStyle = if (index > 0) styles.atOrBefore(index - 1) else null
        if (style == prevStyle) return

        styles[index] = styleCopy()
    }

    /**
     * Remove all styles previously added within the specified range.
     *
     * Although a range will be removed, the style at the end of this range will still be preserved (unless the range
     * also removed the last indexed style).
     *
     * For example, if you had red @ 0, green @ 5, and blue @ 10, and then you remove the range 4 to 6, then you'll
     * have red @ 0, green @ 4, and blue @ 7. "rrrrrgggggbbbbb..." -> "rrrrgggbbbb..." ("rgg" was removed)
     */
    fun removeRange(range: IntRange) {
        val fromInclusive = range.first
        val toInclusive = range.last
        if (styles.isEmpty() || fromInclusive > toInclusive || fromInclusive > styles.lastKey()) return

        var finalStyleInRange: TextStyle? = null
        styles.rangeSequence(range).forEach { (index, _) ->
            finalStyleInRange = styles.remove(index) ?: finalStyleInRange
        }

        var trailingStyleExists = false // If no trailing style exists, we deleted the last style in our range
        val deletedRangeLength = toInclusive - fromInclusive + 1
        styles.rangeSequence(toInclusive + 1 .. Int.MAX_VALUE).forEach { (index, entry) ->
            styles.remove(index)
            styles[index - deletedRangeLength] = entry
            trailingStyleExists = true
        }

        // If there were any styles still remaining AFTER the range we just deleted, that means we need to preserve the
        // last style in the range that was removed.
        finalStyleInRange?.let { finalStyleInRange ->
            if (trailingStyleExists && styles[fromInclusive] == null) {
                val currStyle = styles.atOrBefore(fromInclusive)
                if (currStyle != finalStyleInRange) {
                    styles[fromInclusive] = finalStyleInRange
                }
            }
        }
    }

    /**
     * Reset this class to a default state.
     */
    fun clear() {
        styles.clear()
    }

    override fun at(index: Int): TextStyle = styles.atOrBefore(index)
}

/**
 * A convenience function for putting in a new style initialized via an [init] block.
 *
 * The style will be pre-initialized with the style value that would already have been active at [index] (excluding any
 * style at [index] that you will be overwriting), so you only need to initialize what has changed. Or you can set
 * [fromScratch] to true and get an empty text style instead.
 */
internal fun MutableDocumentStyles.put(
    index: Int,
    fromScratch: Boolean = false,
    init: MutableTextStyle.() -> Unit
): TextStyle {
    val textStyle = createEmptyTextStyle()
    if (!fromScratch && index > 0) {
        textStyle.setFrom(this.at((index - 1)))
    }
    init(textStyle)
    put(index, textStyle)
    return textStyle
}


internal fun MutableDocumentStyles.removeRange(fromInclusive: Int, toExclusive: Int) = removeRange(fromInclusive until toExclusive)
internal fun MutableDocumentStyles.removeAt(index: Int) = removeRange(index, index + 1)
internal fun MutableDocumentStyles.removeFrom(fromInclusive: Int) = removeRange(fromInclusive, Int.MAX_VALUE)

