package com.varabyte.kotterx.util.collections

import com.varabyte.kotterx.util.collections.IndexGroup.Range
import com.varabyte.kotterx.util.collections.IndexGroup.Single

private fun convertRelativeIndex(value: Int, size: Int): Int =
    if (value >= 0) value else size + value

private sealed class IndexGroup {
    abstract fun toIntRange(size: Int): IntRange

    class Range(val start: Int, val endInclusive: Int) : IndexGroup() {
        override fun toIntRange(size: Int): IntRange {
            val startConverted = convertRelativeIndex(start, size)
            val endConverted = convertRelativeIndex(endInclusive, size)
            return startConverted..endConverted
        }
    }

    class Single(val value: Int) : IndexGroup() {
        override fun toIntRange(size: Int): IntRange {
            val valueConverted = convertRelativeIndex(value, size)
            return valueConverted..valueConverted
        }
    }
}

/**
 * A class that represents a collection of indices, allowing for negative values which indicate offsets from the end.
 *
 * For example, if you have a collection of size 10, then adding indices 0, 1, 2, 3, -3, and -1 is equivalent to
 * representing the indices 0, 1, 2, 3, 7, and 9.
 *
 * You can also add ranges as a shortcut. For example, the range [0, -1] means every index in the collection.
 *
 * Once you create an [Indices] instance, you will likely want to [resolve] it, which converts any relative negative
 * indices to absolute values.
 *
 * To create an instance of this class, use the provided [indicesOf] helper methods to build it:
 *
 * ```kotlin
 * val indices = indicesOf {
 *    addRange(0, 3)
 *    add(-3)
 *    add(-1)
 * }.resolve(10)
 *
 * // If you don't specify any ranges, this alternate form of `indicesOf` may be a bit more compact:
 * // val indicies = indicesOf(0, 1, 2, 3, -3, -1).resolve(10)
 * ```
 */
class Indices private constructor(private val indexGroups: List<IndexGroup>) {
    companion object {
        val Empty = Indices(emptyList())
    }
    class Builder {
        private val indexGroups = mutableListOf<IndexGroup>()

        fun add(value: Int) = apply { indexGroups.add(Single(value)) }
        /**
         * Add a range of indices to this collection.
         *
         * Note that invalid ranges (i.e. start > end) will be ignored. We do this instead of throwing an exception
         * because negative indices are allowed here, and we don't know until an [Indices.resolve] happens if, say,
         * index 3 < index -5 or vice versa.
         */
        fun addRange(start: Int, endInclusive: Int) = apply { indexGroups.add(Range(start, endInclusive)) }
        fun addRange(range: IntRange) = addRange(range.first, range.last)

        fun build(): Indices = Indices(indexGroups)
    }

    internal fun toIntRanges(size: Int): List<IntRange> {
        return indexGroups.map { it.toIntRange(size) }
    }

    /**
     * Resolve all indices, converting any relative negative values into absolute values.
     *
     * @param size The size of the collection being indexed (e.g. 10 means indices 0-9 are valid.)
     */
    fun resolve(size: Int = Int.MAX_VALUE) = ResolvedIndices(this, size)
}

/**
 * A snapshot of an [Indices] instance with the [size] value locked in.
 */
class ResolvedIndices internal constructor(indices: Indices, private val size: Int) {
    private val intRanges = indices.toIntRanges(size)

    fun contains(index: Int): Boolean {
        return (index in 0 until size) && intRanges.any { index in it }
    }
}

/**
 * Create an instance of a collection of [Indices] via an init callback.
 *
 * The callback provides access to the [Indices.Builder.add] and [Indices.Builder.addRange] methods.
 *
 * Specifying negative numbers is allowed, and doing so indexes from the last row of your collection, e.g. -1 is another
 * way to specify the very last index and -2 is the second to last.
 */
fun indicesOf(init: Indices.Builder.() -> Unit): Indices {
    val builder = Indices.Builder()
    builder.init()
    return builder.build()
}

/**
 * Create an instance of a collection of [Indices] when you can manually specify all known indices (i.e. no ranges).
 *
 * Specifying negative numbers is allowed, and doing so indexes from the last row of your collection, e.g. -1 is another
 * way to specify the very last index and -2 is the second to last.
 */
fun indicesOf(vararg indices: Int): Indices {
    return indicesOf {
        indices.forEach { add(it) }
    }
}
