package com.varabyte.konsole.core

import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import java.util.concurrent.locks.ReentrantLock
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
class KonsoleList<T> internal constructor(app: KonsoleApp, vararg elements: T) : MutableList<T> {
    private val lock = ReentrantLock()
    @GuardedBy("lock")
    private val delegateList = mutableListOf(*elements)
    // Lazy - just create an additional var which handles all the fancy konsole rerendering logic
    private var modifyCount by app.KonsoleVar(0)

    /**
     * Allow calls to lock the list for a longer time than just a single field at a time, useful for example if
     * modifying many fields at once
     *
     * @param R The result type of any value produced as a side effect of calling [block]; can be `Unit`
     */
    fun <R> withLock(block: KonsoleList<T>.() -> R): R = lock.withLock { this.block() }

    // Immutable functions
    override val size: Int get() = lock.withLock { delegateList.size }
    override fun contains(element: T) = lock.withLock { delegateList.contains(element) }
    override fun containsAll(elements: Collection<T>) = lock.withLock { delegateList.containsAll(elements) }
    override fun get(index: Int): T = lock.withLock { delegateList[index] }
    override fun indexOf(element: T) = lock.withLock { delegateList.indexOf(element) }
    override fun isEmpty() = lock.withLock { delegateList.isEmpty() }
    override fun lastIndexOf(element: T) = lock.withLock { delegateList.lastIndexOf(element) }
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = lock.withLock { delegateList.subList(fromIndex, toIndex) }

    // Iterators
    // Note: We return iterators to the copy of a list, so we don't have to worry about another thread clobbering it

    override fun iterator(): MutableIterator<T> {
        return lock.withLock { delegateList.toMutableList() }.iterator()
    }

    override fun listIterator(): MutableListIterator<T> {
        return lock.withLock { delegateList.toMutableList() }.listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        return lock.withLock { delegateList.toMutableList() }.listIterator(index)
    }

    // Mutable methods
    override fun add(element: T): Boolean {
        return lock.withLock {
            delegateList.add(element).also { modifyCount++ }
        }
    }

    override fun add(index: Int, element: T) {
        return lock.withLock {
            delegateList.add(index, element).also { modifyCount++ }
        }
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        return lock.withLock {
            delegateList.addAll(index, elements).also { modifyCount++ }
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return lock.withLock {
            delegateList.addAll(elements).also { modifyCount++ }
        }
    }

    override fun clear() {
        return lock.withLock {
            delegateList.clear().also { modifyCount++ }
        }
    }

    override fun remove(element: T): Boolean {
        return lock.withLock {
            delegateList.remove(element).also { modifyCount++ }
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return lock.withLock {
            delegateList.removeAll(elements).also { modifyCount++ }
        }
    }

    override fun removeAt(index: Int): T {
        return lock.withLock {
            delegateList.removeAt(index).also { modifyCount++ }
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return lock.withLock {
            delegateList.retainAll(elements).also { modifyCount++ }
        }
    }

    override fun set(index: Int, element: T): T {
        return lock.withLock {
            delegateList.set(index, element).also { modifyCount++ }
        }
    }
}