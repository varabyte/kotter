package com.varabyte.konsole.core

import com.varabyte.konsole.core.KonsoleData.Key
import com.varabyte.konsole.core.KonsoleData.Lifecycle
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
    /**
     * A marker interface for an object that represents a class's lifecycle. For example, you might declare something
     * like so:
     *
     * ```
     * class Application {
     *    object Lifecycle : KonsoleData.Lifecycle
     * }
     * ```
     *
     * and then, if you ever create a new key for some unique data you want to add, you can write:
     *
     * ```
     * object CredentialsKey : KonsoleData.Key<String> {
     *   override val lifecycle = Application.Lifecycle
     * }
     *
     * data[CredentialsKey] = UUID.randomUUID().toString()
     * ```
     */
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

    /**
     * Dispose and remove all keys tied to the specified [Lifecycle]
     */
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

    /**
     * Access the stored value directly.
     *
     * Warning: Be careful with this method, as you are accessing data from outside a lock it is normally stored behind.
     * Prefer using the [get] method which takes a block for maximum safetly.
     */
    operator fun <T: Any> get(key: Key<T>): T? {
        return lock.withLock {
            (keyValues[key] as? Value<T>)?.wrapped
        }
    }

    /**
     * Access the stored value within a scoped block, during which a lock protecting all data is held.
     *
     * This might silently do nothing if no data was found with the key. If you want to ensure the block always runs,
     * consider using [putIfAbsent] instead.
     */
    fun <T : Any> get(key: Key<T>, block: T.() -> Unit) {
        lock.withLock {
            this[key]?.let { value -> value.block() }
        }
    }

    operator fun <T: Any> set(key: Key<T>, value: T) {
        set(key, value, dispose = {})
    }

    /**
     * Set the stored value directly.
     *
     * It is expected users will prefer to use [tryPut] or [putIfAbsent] methods instead, treating this data block like
     * a cache of lazily instantiated values, but this method is provided in case direct setting is needed.
     */
    fun <T: Any> set(key: Key<T>, value: T, dispose: (T) -> Unit) {
        return lock.withLock {
            keyValues[key] = Value(value, dispose)
        }
    }


    /**
     * A convenience version of the other [tryPut] but without a dispose block, making the syntax for this common
     * case nicer.
     */
    fun <T : Any> tryPut(key: Key<T>, provideInitialValue: () -> T): Boolean {
        return tryPut(key, provideInitialValue, dispose = {})
    }

    /**
     * Like [putIfAbsent], but returns a boolean instead of triggering a block.
     *
     * Returns true if the value was added, false if something already existed with the associated key.
     */
    fun <T : Any> tryPut(key: Key<T>, provideInitialValue: () -> T, dispose: (T) -> Unit): Boolean {
        return lock.withLock {
            var wasPut = false
            keyValues.computeIfAbsent(key) { wasPut = true; Value(provideInitialValue(), dispose) }
            wasPut
        }
    }

    /**
     * Run the target [block] with the current value inside this data store, or if one doesn't exist, trigger
     * [provideInitialValue] to generate it. This ensured that [block] will always be called. An optional [dispose]
     * block can be provided.
     */
    fun <T : Any> putIfAbsent(key: Key<T>, provideInitialValue: () -> T, dispose: (T) -> Unit = {}, block: (T) -> Unit = {}) {
        return lock.withLock {
            (keyValues.computeIfAbsent(key) { Value(provideInitialValue(), dispose) } as T?)?.let { block(it) }
        }
    }
}