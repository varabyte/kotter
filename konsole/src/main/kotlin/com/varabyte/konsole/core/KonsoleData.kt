package com.varabyte.konsole.core

import net.jcip.annotations.GuardedBy
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A thread-safe package of key/value pairs.
 *
 * You should register data using a predefined [Key] object. When registering values, an optional dispose block can be
 * specified at the same time, which will automatically be triggered when the block is done running.
 *
 * This data will be tied to a [Lifecycle]. Call [dispose] to remove all keys with the matching lifecycle.
 */
@Suppress("UNCHECKED_CAST")
class KonsoleData {
    interface Lifecycle
    interface Key<T> {
        val lifecycle: Lifecycle
    }

    private class Value<T>(val wrapped: T, private val dispose: (T) -> Unit = {}) {
        fun dispose() {
            dispose(wrapped)
        }
    }

    private val lock = ReentrantLock()
    @GuardedBy("lock")
    private val keyValues = mutableMapOf<Key<out Any>, Value<out Any>>()

    fun dispose(lifecycle: Lifecycle) {
        lock.withLock {
            keyValues.entries.removeAll { entry ->
                if (entry.key.lifecycle === lifecycle) {
                    entry.value.dispose()
                    true
                }
                else {
                    false
                }
            }
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