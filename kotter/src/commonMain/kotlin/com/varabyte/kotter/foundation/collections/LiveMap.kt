package com.varabyte.kotter.foundation.collections

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.platform.concurrent.locks.*
import com.varabyte.kotter.platform.internal.concurrent.annotations.*
import com.varabyte.kotter.runtime.*

/**
 * Like [LiveVar], but for maps.
 *
 * In other words, adding or removing to the map will cause the active section to rerender automatically.
 *
 * For example:
 *
 * ```
 * val num2Names = liveMapOf(1 to "one", 2 to "two", 3 to "three")
 * section {
 *     num2Names.forEach { (num, name) -> textLine("$num is spelled \"$name\"") }
 * }.run {
 *     num2Names.putAll(mapOf(4 to "four", 5 to "five", 6 to "six"))
 * }
 * ```
 *
 * This class is thread safe and expected to be accessed across different threads.
 */
@ThreadSafe
class LiveMap<K, V> internal constructor(private val session: Session, vararg elements: Pair<K, V>) : MutableMap<K, V> {

    private inner class SafeMutableIterator<T>(private val wrapped: MutableIterator<T>) : MutableIterator<T> {
        override fun remove() {
            write { wrapped.remove() }
        }

        override fun next(): T {
            return read { wrapped.next() }
        }

        override fun hasNext(): Boolean {
            return read { wrapped.hasNext() }
        }
    }

    @Suppress("ConvertArgumentToSet")
    private inner class SafeMutableEntries<K, V>(private val wrapped: MutableSet<MutableMap.MutableEntry<K, V>>): MutableSet<MutableMap.MutableEntry<K, V>> {
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
            return SafeMutableIterator(wrapped.iterator())
        }

        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
            throw UnsupportedOperationException() // Kotlin mutableMap entries does not support addition
        }

        override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
            return write { wrapped.remove(element) }
        }

        override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            throw UnsupportedOperationException() // Kotlin mutableMap entries does not support addition
        }

        override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            return write { wrapped.removeAll(elements) }
        }

        override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            return write { wrapped.retainAll(elements) }
        }

        override fun clear() {
            write { wrapped.clear() }
        }

        override val size: Int
            get() = read { wrapped.size }

        override fun isEmpty(): Boolean {
            return read { wrapped.isEmpty()}
        }

        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean {
            return read { wrapped.contains(element) }
        }

        override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            return read { wrapped.containsAll(elements) }
        }
    }

    @Suppress("ConvertArgumentToSet")
    private inner class SafeMutableSet<T>(private val wrapped: MutableSet<T>): MutableSet<T> {
        override fun iterator(): MutableIterator<T> {
            return SafeMutableIterator(wrapped.iterator())
        }

        override fun add(element: T): Boolean {
            throw UnsupportedOperationException() // Kotlin mutableMap keys does not support addition
        }

        override fun remove(element: T): Boolean {
            return write { wrapped.remove(element) }
        }

        override fun addAll(elements: Collection<T>): Boolean {
            throw UnsupportedOperationException() // Kotlin mutableMap keys does not support addition
        }

        override fun removeAll(elements: Collection<T>): Boolean {
            return write { wrapped.removeAll(elements) }
        }

        override fun retainAll(elements: Collection<T>): Boolean {
            return write { wrapped.retainAll(elements) }
        }

        override fun clear() {
            write { wrapped.clear() }
        }

        override val size: Int
            get() = read { wrapped.size }

        override fun isEmpty(): Boolean {
            return read { wrapped.isEmpty() }
        }

        override fun contains(element: T): Boolean {
            return read { wrapped.contains(element) }
        }

        override fun containsAll(elements: Collection<T>): Boolean {
            return read { wrapped.containsAll(elements) }
        }
    }

    @Suppress("ConvertArgumentToSet")
    private inner class SafeMutableCollection<T>(private val wrapped: MutableCollection<T>): MutableCollection<T> {
        override fun iterator(): MutableIterator<T> {
            return SafeMutableIterator(wrapped.iterator())
        }

        override fun add(element: T): Boolean {
            return write { wrapped.add(element) }
        }

        override fun remove(element: T): Boolean {
            return write { wrapped.remove(element) }
        }

        override fun addAll(elements: Collection<T>): Boolean {
            return write { wrapped.addAll(elements) }
        }

        override fun removeAll(elements: Collection<T>): Boolean {
            return write { wrapped.removeAll(elements) }
        }

        override fun retainAll(elements: Collection<T>): Boolean {
            return write { wrapped.retainAll(elements) }
        }

        override fun clear() {
            return write { wrapped.clear() }
        }

        override val size: Int
            get() = read { wrapped.size }

        override fun isEmpty(): Boolean {
            return read { wrapped.isEmpty() }
        }

        override fun contains(element: T): Boolean {
            return read { wrapped.contains(element) }
        }

        override fun containsAll(elements: Collection<T>): Boolean {
            return read { wrapped.containsAll(elements) }
        }
    }

    // LiveVar already has a lot of nice logic for updating the render block as necessary, so we delegate to it to
    // avoid reimplementing the logic here
    private var modifyCountVar by session.liveVarOf(0)
    private var modifyCount = 0

    @GuardedBy("session.data.lock")
    private val delegateMap = mutableMapOf(*elements)

    private fun <R> read(block: () -> R): R {
        return withReadLock {
            // Triggers LiveVar.getValue but not setValue (which, here, aborts early because value is the same)
            @Suppress("SelfAssignment")
            modifyCountVar = modifyCountVar
            block()
        }
    }

    private fun <R> write(block: () -> R): R {
        return withWriteLock {
            // Triggers LiveVar.setValue but not getValue
            modifyCountVar = ++modifyCount
            block()
        }
    }

    /**
     * Allow calls to lock the map for a longer time than just a single field at a time, useful if reading many fields
     * at once.
     *
     * @param R The result type of any value produced as a side effect of calling [block]; can be `Unit`
     */
    fun <R> withReadLock(block: LiveMap<K, V>.() -> R): R = session.data.lock.read { this.block() }

    /**
     * Allow calls to write lock the map for a longer time than just a single field at a time, useful if
     * updating many fields at once.
     *
     * @param R The result type of any value produced as a side effect of calling [block]; can be `Unit`
     */
    fun <R> withWriteLock(block: LiveMap<K, V>.() -> R): R = session.data.lock.write { this.block() }

    // Immutable methods
    override val size get() = read { delegateMap.size }
    override fun isEmpty() = read { delegateMap.isEmpty() }
    override fun containsKey(key: K) = read { delegateMap.containsKey(key) }
    override fun containsValue(value: V) = read { delegateMap.containsValue(value) }
    override fun get(key: K) = read { delegateMap[key] }

    // Access to inner contents
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = SafeMutableEntries(delegateMap.entries)
    override val keys: MutableSet<K> get() = SafeMutableSet(delegateMap.keys)
    override val values: MutableCollection<V> get() = SafeMutableCollection(delegateMap.values)

    // Mutable methods
    override fun clear() = write { delegateMap.clear() }
    override fun remove(key: K) = write { delegateMap.remove(key) }
    override fun putAll(from: Map<out K, V>) = write { delegateMap.putAll(from) }
    override fun put(key: K, value: V) = write { delegateMap.put(key, value) }
}

/** Create a [LiveMap] whose scope is tied to this session. */
fun <K, V> Session.liveMapOf(vararg elements: Pair<K, V>): LiveMap<K, V> = LiveMap(this, *elements)
fun <K, V> Session.liveMapOf(elements: Iterable<Pair<K, V>>) = liveMapOf(*elements.toList().toTypedArray())
