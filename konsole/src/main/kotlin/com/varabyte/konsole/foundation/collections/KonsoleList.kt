package com.varabyte.konsole.foundation.collections

import com.varabyte.konsole.foundation.KonsoleVar
import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.runtime.KonsoleApp
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import kotlin.concurrent.withLock

/**
 * Like [KonsoleVar], but for lists.
 *
 * In other words, adding or removing to the list will cause the konsole block to rerender automatically.
 *
 * ```
 * val ids = KonsoleList<String>()
 *
 * konsole {
 *   for (id in ids) { ... }
 * }.run {
 *   ids.add(...)
 * }
 * ```
 *
 * This class's value can be queried and modified across different values, so it is designed to be thread safe.
 */
@ThreadSafe
class KonsoleList<T> internal constructor(private val app: KonsoleApp, vararg elements: T) : MutableList<T> {
    // KonsoleVar already has a lot of nice logic for updating the render block as necessary, so we delegate to it to
    // avoid reimplementing the logic here
    private var modifyCountVar by app.konsoleVarOf(0)
    private var modifyCount = 0

    @GuardedBy("app.data.lock")
    private val delegateList = mutableListOf(*elements)

    private fun <R> readLock(block: () -> R): R {
        return app.data.lock.withLock {
            // Triggers KonsoleVar.getValue but not setValue (which, here, aborts early because value is the same)
            modifyCountVar = modifyCountVar
            block()
        }
    }

    private fun <R> writeLock(block: () -> R): R {
        return app.data.lock.withLock {
            // Triggers KonsoleVar.setValue but not getValue
            modifyCountVar = ++modifyCount
            block()
        }
    }

    /**
     * Allow calls to lock the list for a longer time than just a single field at a time, useful for example if
     * reading or updating many fields at once.
     *
     * @param R The result type of any value produced as a side effect of calling [block]; can be `Unit`
     */
    fun <R> withLock(block: KonsoleList<T>.() -> R): R = app.data.lock.withLock { this.block() }

    // Immutable functions
    override val size: Int get() = readLock { delegateList.size }
    override fun contains(element: T) = readLock { delegateList.contains(element) }
    override fun containsAll(elements: Collection<T>) = readLock { delegateList.containsAll(elements) }
    override fun get(index: Int): T = readLock { delegateList[index] }
    override fun indexOf(element: T) = readLock { delegateList.indexOf(element) }
    override fun isEmpty() = readLock { delegateList.isEmpty() }
    override fun lastIndexOf(element: T) = readLock { delegateList.lastIndexOf(element) }
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = readLock { delegateList.subList(fromIndex, toIndex) }

    // Iterators
    // Note: We return iterators to the copy of a list, so we don't have to worry about another thread clobbering it
    override fun iterator(): MutableIterator<T> = readLock { delegateList.toMutableList() }.iterator()
    override fun listIterator(): MutableListIterator<T> = readLock { delegateList.toMutableList() }.listIterator()
    override fun listIterator(index: Int): MutableListIterator<T> = readLock { delegateList.toMutableList() }.listIterator(index)

    // Mutable methods
    override fun add(element: T): Boolean = writeLock { delegateList.add(element) }
    override fun add(index: Int, element: T) = writeLock { delegateList.add(index, element) }
    override fun addAll(index: Int, elements: Collection<T>): Boolean =
        writeLock { delegateList.addAll(index, elements) }
    override fun addAll(elements: Collection<T>): Boolean = writeLock { delegateList.addAll(elements) }
    override fun clear() = writeLock { delegateList.clear() }
    override fun remove(element: T): Boolean = writeLock { delegateList.remove(element) }
    override fun removeAll(elements: Collection<T>): Boolean = writeLock { delegateList.removeAll(elements) }
    override fun removeAt(index: Int): T = writeLock { delegateList.removeAt(index) }
    override fun retainAll(elements: Collection<T>): Boolean = writeLock { delegateList.retainAll(elements) }
    override fun set(index: Int, element: T): T = writeLock { delegateList.set(index, element) }
}

/** Create a [KonsoleList] whose scope is tied to this app. */
@Suppress("FunctionName") // Intentionally made to look like a class constructor
fun <T> KonsoleApp.konsoleListOf(vararg elements: T): KonsoleList<T> = KonsoleList<T>(this, *elements)