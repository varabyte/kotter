package com.varabyte.kotter.foundation.collections

import com.varabyte.kotter.foundation.LiveVar
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.runtime.Session
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Like [LiveVar], but for lists.
 *
 * In other words, adding or removing to the list will cause the active section to rerender automatically.
 *
 * ```
 * val ids = liveListOf<String>()
 *
 * section {
 *   for (id in ids) { ... }
 * }.run {
 *   ids.add(...)
 * }
 * ```
 *
 * This class's value can be queried and modified across different values, so it is designed to be thread safe.
 */
@ThreadSafe
class LiveList<T> internal constructor(private val app: Session, vararg elements: T) : MutableList<T> {
    // LiveVar already has a lot of nice logic for updating the render block as necessary, so we delegate to it to
    // avoid reimplementing the logic here
    private var modifyCountVar by app.liveVarOf(0)
    private var modifyCount = 0

    @GuardedBy("app.data.lock")
    private val delegateList = mutableListOf(*elements)

    private fun <R> read(block: () -> R): R {
        return app.data.lock.read {
            // Triggers LiveVar.getValue but not setValue (which, here, aborts early because value is the same)
            modifyCountVar = modifyCountVar
            block()
        }
    }

    private fun <R> write(block: () -> R): R {
        return app.data.lock.write {
            // Triggers LiveVar.setValue but not getValue
            modifyCountVar = ++modifyCount
            block()
        }
    }

    /**
     * Allow calls to lock the list for a longer time than just a single field at a time, useful if reading many fields
     * at once.
     *
     * @param R The result type of any value produced as a side effect of calling [block]; can be `Unit`
     */
    fun <R> withReadLock(block: LiveList<T>.() -> R): R = app.data.lock.read { this.block() }

    /**
     * Allow calls to write lock the list for a longer time than just a single field at a time, useful if
     * updating many fields at once.
     *
     * @param R The result type of any value produced as a side effect of calling [block]; can be `Unit`
     */
    fun <R> withWriteLock(block: LiveList<T>.() -> R): R = app.data.lock.write { this.block() }

    // Immutable functions
    override val size: Int get() = read { delegateList.size }
    override fun contains(element: T) = read { delegateList.contains(element) }
    override fun containsAll(elements: Collection<T>) = read { delegateList.containsAll(elements) }
    override fun get(index: Int): T = read { delegateList[index] }
    override fun indexOf(element: T) = read { delegateList.indexOf(element) }
    override fun isEmpty() = read { delegateList.isEmpty() }
    override fun lastIndexOf(element: T) = read { delegateList.lastIndexOf(element) }
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
        read { delegateList.subList(fromIndex, toIndex) }

    // Iterators
    // Note: We return iterators to the copy of a list, so we don't have to worry about another thread clobbering it
    override fun iterator(): MutableIterator<T> = read { delegateList.toMutableList() }.iterator()
    override fun listIterator(): MutableListIterator<T> = read { delegateList.toMutableList() }.listIterator()
    override fun listIterator(index: Int): MutableListIterator<T> = read { delegateList.toMutableList() }
        .listIterator(index)

    // Mutable methods
    override fun add(element: T): Boolean = write { delegateList.add(element) }
    override fun add(index: Int, element: T) = write { delegateList.add(index, element) }
    override fun addAll(index: Int, elements: Collection<T>): Boolean = write { delegateList.addAll(index, elements) }
    override fun addAll(elements: Collection<T>): Boolean = write { delegateList.addAll(elements) }
    override fun clear() = write { delegateList.clear() }
    override fun remove(element: T): Boolean = write { delegateList.remove(element) }
    override fun removeAll(elements: Collection<T>): Boolean = write { delegateList.removeAll(elements) }
    override fun removeAt(index: Int): T = write { delegateList.removeAt(index) }
    override fun retainAll(elements: Collection<T>): Boolean = write { delegateList.retainAll(elements) }
    override fun set(index: Int, element: T): T = write { delegateList.set(index, element) }
}

/** Create a [LiveList] whose scope is tied to this app. */
@Suppress("FunctionName") // Intentionally made to look like a class constructor
fun <T> Session.liveListOf(vararg elements: T): LiveList<T> = LiveList<T>(this, *elements)