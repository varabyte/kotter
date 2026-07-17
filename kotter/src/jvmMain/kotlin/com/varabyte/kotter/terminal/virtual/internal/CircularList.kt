package com.varabyte.kotter.terminal.virtual.internal

/**
 * A simple circular buffer implementation, designed to allow lightweight insertion / removal from the front of even
 * massively large lists without incurring expensive item shifting.
 */
class CircularList<T : Any>(initialSize: Int = DEFAULT_SIZE) : AbstractMutableList<T>() {
    companion object {
        const val MINIMUM_SIZE = 4
        const val DEFAULT_SIZE = 8
    }
    init {
        require(initialSize >= MINIMUM_SIZE) { "Circular buffers only make sense with more than a few elements in them"}
    }

    private fun newBuffer(size: Int): ArrayList<T?> {
        require(size > 0) { "CircularBuffer size must be >= $MINIMUM_SIZE; got $size" }
        return ArrayList<T?>(size).apply { repeat(size) { add(null) } }
    }

    private var buffer = newBuffer(initialSize)
    private var startInclusive = 0
    private var endExclusive = 0

    private fun toExternalIndex(internalIndex: Int): Int {
        if (internalIndex < 0) return internalIndex // neg index means not found, that translates directly to external
        return (internalIndex - startInclusive).let { if (it >= 0) it else it + buffer.size }
    }

    private fun toInternalIndex(externalIndex: Int): Int {
        check(externalIndex >= 0) // We don't expect -1 indices from users
        return (startInclusive + externalIndex).let { if (it <= buffer.lastIndex) it else it - buffer.size}
    }

    private fun Int.incWrap(): Int {
        return if (this == buffer.lastIndex) 0 else this + 1
    }

    private fun Int.decWrap(): Int {
        return if (this == 0) buffer.lastIndex else this - 1
    }

    private fun growBuffer() {
        val newBuffer = newBuffer(buffer.size * 2)
        val numItems = size
        this.forEachIndexed { i, item ->
            newBuffer[i] = item
        }
        buffer = newBuffer
        startInclusive = 0
        endExclusive = numItems
    }

    private fun growIfNecessary(): Boolean {
        // Grow just before we totally fill up the buffer; if we get to that point, startInclusive and endExclusive will
        // point at the same index and that is the same as when we're empty. We could distinguish those two cases but it
        // it easier if we just grow a little earlier.
        return if (size == buffer.size - 1) { growBuffer(); true } else false
    }

    override fun add(element: T): Boolean {
        add(size, element)
        return true
    }

    override fun clear() {
        buffer.fill(null)
        startInclusive = 0
        endExclusive = 0
    }

    override fun set(index: Int, element: T): T {
        if (index !in 0..lastIndex) throw IndexOutOfBoundsException(index)
        val setIndex = toInternalIndex(index)
        val replaced = buffer.set(setIndex, element)
        // nulls only occur for regions out of bounds
        return replaced!!
    }

    override fun add(index: Int, element: T) {
        if (index !in 0..size) throw IndexOutOfBoundsException(index)
        growIfNecessary()

        // Figure out if we want to shift leading elements down or trailing elements back; whichever is smaller
        val diffToStart = index
        val diffToEnd = size - index

        // Bias towards appending; it makes the math easier when debugging
        if (diffToStart >= diffToEnd) {
            var itemsToMove = diffToEnd
            var moveIntoIndex = endExclusive
            endExclusive = endExclusive.incWrap()
            while (itemsToMove > 0) {
                val moveOutOfIndex = moveIntoIndex.decWrap()
                buffer[moveIntoIndex] = buffer[moveOutOfIndex]
                buffer[moveOutOfIndex] = null
                moveIntoIndex = moveOutOfIndex
                --itemsToMove
            }
        } else {
            var itemsToMove = diffToStart
            startInclusive = startInclusive.decWrap()
            var moveIntoIndex = startInclusive
            while (itemsToMove > 0) {
                val moveOutOfIndex = moveIntoIndex.incWrap()
                buffer[moveIntoIndex] = buffer[moveOutOfIndex]
                buffer[moveOutOfIndex] = null
                moveIntoIndex = moveOutOfIndex
                --itemsToMove
            }
        }
        buffer[toInternalIndex(index)] = element
    }

    fun addFirst(element: T) {
        add(0, element)
    }

    fun removeFirst(): T {
        return removeAt(0)
    }

    fun removeLast(): T {
        return removeAt(lastIndex)
    }

    override fun removeAt(index: Int): T {
        if (index !in 0..lastIndex) throw IndexOutOfBoundsException(index)

        val removeIndex = toInternalIndex(index)
        val removed = buffer[removeIndex]!!
        buffer[removeIndex] = null

        // Figure out if we want to shift leading elements up or trailing elements down; whichever is smaller
        val diffToStart = index
        val diffToEnd = size - index

        var moveIntoIndex = removeIndex
        if (diffToStart <= diffToEnd) {
            startInclusive = startInclusive.incWrap()
            var itemsToMove = diffToStart
            while (itemsToMove > 0) {
                val moveFromIndex = moveIntoIndex.decWrap()
                buffer[moveIntoIndex] = buffer[moveFromIndex]
                buffer[moveFromIndex] = null
                moveIntoIndex = moveFromIndex
                --itemsToMove
            }
        } else {
            endExclusive = endExclusive.decWrap()
            var itemsToMove = diffToEnd
            while (itemsToMove > 0) {
                val moveFromIndex = moveIntoIndex.incWrap()
                buffer[moveIntoIndex] = buffer[moveFromIndex]
                buffer[moveFromIndex] = null
                moveIntoIndex = moveFromIndex
                --itemsToMove
            }
        }
        return removed
    }

    override val size: Int
        get() {
            return toExternalIndex(endExclusive)
        }

    override fun get(index: Int): T {
        // nulls only occur for regions out of bounds
        return buffer[toInternalIndex(index)]!!
    }
}