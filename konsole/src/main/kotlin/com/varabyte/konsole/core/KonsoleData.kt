package com.varabyte.konsole.core

import net.jcip.annotations.GuardedBy
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface Key<T>

/**
 * A thread-safe package of key/value pairs.
 *
 * This data will be associated with a block and can be accessed from various scopes in a thread-safe manner.
 */
@Suppress("UNCHECKED_CAST")
class KonsoleData {
    private val lock = ReentrantLock()
    @GuardedBy("lock")
    private val keyValues = mutableMapOf<Key<out Any>, Any>()

    operator fun <T : Any> set(key: Key<T>, value: T) {
        lock.withLock {
            val oldValue = keyValues.putIfAbsent(key, value)
            check(oldValue != null) { "Cannot override data after it has been initially set" }
        }
    }

    operator fun <T : Any> get(key: Key<T>): T? {
        return lock.withLock {
            keyValues[key] as T?
        }
    }

    fun <T : Any> getValue(key: Key<T>): T = this[key]!!

    fun <T : Any> get(key: Key<T>, block: (T) -> Unit) {
        lock.withLock {
            this[key]?.let { block(it) }
        }
    }

    fun <T : Any> getOrPut(key: Key<T>, provideInitialValue: () -> T, block: (T) -> Unit) {
        lock.withLock {
            (keyValues.computeIfAbsent(key) { provideInitialValue() } as T?)?.let { block(it) }
        }
    }
}