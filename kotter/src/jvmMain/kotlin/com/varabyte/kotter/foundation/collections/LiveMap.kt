package com.varabyte.kotter.foundation.collections

import com.varabyte.kotter.foundation.LiveVar
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.runtime.Session
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import kotlin.concurrent.read
import kotlin.concurrent.write

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
    // LiveVar already has a lot of nice logic for updating the render block as necessary, so we delegate to it to
    // avoid reimplementing the logic here
    private var modifyCountVar by session.liveVarOf(0)
    private var modifyCount = 0

    @GuardedBy("session.data.lock")
    private val delegateMap = mutableMapOf(*elements)

    private fun <R> read(block: () -> R): R {
        return withReadLock {
            // Triggers LiveVar.getValue but not setValue (which, here, aborts early because value is the same)
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


    // Technically mutable methods that we treat as immutable
    // This is a downgrade for what the MutableMap API is supposed to expose, but it's a pain to implement this
    // correctly in a way that ensures we won't do a ton of unecessary rerendering of sections. Most of the time users
    // are using these fields for read only purposes.
    override val entries get() = read { delegateMap.toMutableMap().entries }
    override val keys get() = read { delegateMap.toMutableMap().keys }
    override val values get() = read { delegateMap.toMutableMap().values }

    // Mutable methods
    override fun clear() = write { delegateMap.clear() }
    override fun remove(key: K) = write { delegateMap.remove(key) }
    override fun putAll(from: Map<out K, V>) = write { delegateMap.putAll(from) }
    override fun put(key: K, value: V) = write { delegateMap.put(key, value) }
}

/** Create a [LiveMap] whose scope is tied to this session. */
fun <K, V> Session.liveMapOf(vararg elements: Pair<K, V>): LiveMap<K, V> = LiveMap(this, *elements)
fun <K, V> Session.liveMapOf(elements: Iterable<Pair<K, V>>) = liveMapOf(*elements.toList().toTypedArray())