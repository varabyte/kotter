package com.varabyte.konsole.core

import net.jcip.annotations.GuardedBy
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A thread-safe package of key/value pairs.
 *
 * When registering values, an optional dispose block can be registered at the same time, which will automatically be
 * triggered when the block is done running.
 *
 * This data will be associated with a [KonsoleBlock] and is tied to its lifecycle.
 */
@Suppress("UNCHECKED_CAST")
class KonsoleData {
    interface Key<T>
    private class Value<T>(val wrapped: T, private val dispose: (T) -> Unit = {}) {
        fun dispose() {
            dispose(wrapped)
        }
    }

    private val lock = ReentrantLock()
    @GuardedBy("lock")
    private val keyValues = mutableMapOf<Key<out Any>, Value<out Any>>()

    fun dispose() {
        lock.withLock {
            keyValues.values.forEach { value -> value.dispose() }
            keyValues.clear()
        }
    }

    fun <T : Any> get(key: Key<T>, block: T.() -> Unit) {
        lock.withLock {
            (keyValues[key] as? Value<T>)?.let { value -> value.wrapped.block() }
        }
    }

    fun <T : Any> putIfAbsent(key: Key<T>, provideInitialValue: () -> T) {
        putIfAbsent(key, provideInitialValue, dispose = {})
    }

    fun <T : Any> putIfAbsent(key: Key<T>, provideInitialValue: () -> T, dispose: (T) -> Unit) {
        lock.withLock {
            keyValues.computeIfAbsent(key) { Value(provideInitialValue(), dispose) }
        }
    }

    fun <T : Any> getOrPut(key: Key<T>, provideInitialValue: () -> T, dispose: (T) -> Unit = {}, block: (T) -> Unit = {}) {
        lock.withLock {
            (keyValues.computeIfAbsent(key) { Value(provideInitialValue(), dispose) } as T?)?.let { block(it) }
        }
    }
}