package com.varabyte.kotter.foundation.collections

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.runtime.Session
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Like [LiveList], but for sets.
 *
 * In other words, adding or removing to the set will cause the active section to rerender automatically.
 *
 * ```
 * val ids = liveSetOf<String>()
 *
 * section {
 *   for (id in ids) { ... }
 * }.run {
 *   ids.add(...)
 * }
 * ```
 *
 * This class's value can be queried and modified across different threads, so it is designed to be thread safe.
 */
@ThreadSafe
class LiveSet<T> internal constructor(private val session: Session, vararg elements: T) : MutableSet<T> {
    // LiveVar already has a lot of nice logic for updating the render block as necessary, so we delegate to it to
    // avoid reimplementing the logic here
    private var modifyCountVar by session.liveVarOf(0)
    private var modifyCount = 0

    @GuardedBy("session.data.lock")
    private val delegateSet = mutableSetOf(*elements)

    private fun <R> read(block: () -> R): R {
        return session.data.lock.read {
            // Triggers LiveVar.getValue but not setValue (which, here, aborts early because value is the same)
            modifyCountVar = modifyCountVar
            block()
        }
    }

    private fun <R> write(block: () -> R): R {
        return session.data.lock.write {
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
    fun <R> withReadLock(block: LiveSet<T>.() -> R): R = session.data.lock.read { this.block() }

    /**
     * Allow calls to write lock the list for a longer time than just a single field at a time, useful if
     * updating many fields at once.
     *
     * @param R The result type of any value produced as a side effect of calling [block]; can be `Unit`
     */
    fun <R> withWriteLock(block: LiveSet<T>.() -> R): R = session.data.lock.write { this.block() }

    // Immutable methods
    override val size get() = read { delegateSet.size }
    override fun isEmpty() = read { delegateSet.isEmpty() }
    override fun containsAll(elements: Collection<T>) = read { delegateSet.containsAll(elements) }
    override fun contains(element: T) = read { delegateSet.contains(element) }

    // Iterators
    // Note: We return iterators to the copy of this set, so we don't have to worry about another thread clobbering it
    override fun iterator() = read { delegateSet.toMutableSet().iterator() }

    // Mutable methods
    override fun add(element: T) = write { delegateSet.add(element) }
    override fun addAll(elements: Collection<T>) = write { delegateSet.addAll(elements) }
    override fun clear() = write { delegateSet.clear() }

    override fun retainAll(elements: Collection<T>) = write { delegateSet.retainAll(elements) }
    override fun removeAll(elements: Collection<T>) = write { delegateSet.removeAll(elements) }
    override fun remove(element: T) = write { delegateSet.remove(element) }
}

/** Create a [LiveSet] whose scope is tied to this session. */
fun <T> Session.liveSetOf(vararg elements: T): LiveSet<T> = LiveSet<T>(this, *elements)