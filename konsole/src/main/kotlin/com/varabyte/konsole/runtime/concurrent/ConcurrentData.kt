package com.varabyte.konsole.runtime.concurrent

import com.varabyte.konsole.runtime.concurrent.ConcurrentData.Key
import com.varabyte.konsole.runtime.concurrent.ConcurrentData.Lifecycle
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A thread-safe package of key/value pairs.
 *
 * You should register data using a predefined [Key] object. When registering values, an optional dispose block can be
 * specified at the same time, which will automatically be triggered when the block is done running.
 *
 * This data will be tied to a [Lifecycle]. Call [dispose] to remove all keys with the matching lifecycle.
 *
 * Note: Although the class is designed to be thread safe, some calls (which are noted with warnings) can expose values
 * to you which are no longer covered by the lock. Those should only be used to fetch immutable or thread-safe values.
 */
@ThreadSafe
@Suppress("UNCHECKED_CAST")
class ConcurrentData {
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

    /**
     * Occasionally, a user may want to put data in this cache which is expensive to calculate that should only run on
     * demand. To do that, instead of:
     *
     * ```
     * data[ExpensiveKey] = ExpensiveObject()
     * ```
     *
     * the can write:
     *
     * ```
     * data.lazyInitializers[ExpensiveKey] = { ExpensiveObject() }
     * ```
     *
     * Later, a call to ```data[ExpensiveKey]``` will trigger the creation on the fly.
     *
     * Note that if someone else uses one of the set or various put methods between adding a lazy initializer but before
     * the first time the key was accessed, the values those calls add will take precedence.
     */
    class LazyInitializers(private val lock: ReentrantLock) {
        @GuardedBy("lock")
        private val lazyInitializers = mutableMapOf<Key<out Any>, () -> Value<out Any>>()
        operator fun <T: Any> set(key: Key<T>, initialize: () -> T) {
            set(key, initialize, dispose = {})
        }
        fun <T: Any> set(key: Key<T>, initialize: () -> T, dispose: (T) -> Unit) {
            lock.withLock { lazyInitializers[key] = { Value(initialize(), dispose) } }
        }
        internal operator fun <T: Any> get(key: Key<T>): (() -> Value<T>)? {
            return lock.withLock { lazyInitializers[key] as? () -> Value<T> }
        }
    }

    /**
     * A value entry inside the key/value store.
     *
     * Having a separate class for it lets us store the `dispose` callback and also helps us avoid doing some extra
     * type casting in order to line up the `wrapped` value with the disposal call.
     */
    internal class Value<T>(val wrapped: T, private val dispose: (T) -> Unit = {}) {
        fun dispose() {
            dispose(wrapped)
        }
    }

    private val lock = ReentrantLock()
    @GuardedBy("lock")
    private val keyValues = mutableMapOf<Key<out Any>, Value<out Any>>()
    val lazyInitializers = LazyInitializers(lock)

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
     * Prefer using the [get] method which takes a block for maximum safety.
     */
    operator fun <T : Any> get(key: Key<T>): T? {
        return lock.withLock {
            val value = keyValues[key] as? Value<T> ?: run {
                val lazy = lazyInitializers[key]?.invoke()?.apply {
                    keyValues[key] = this
                }
                lazy
            }
            value?.wrapped
        }
    }

    /**
     * Access the stored value directly, when you know for a fact it was set previously.
     *
     * Warning: Be careful with this method, as you are accessing data from outside a lock it is normally stored behind.
     * Prefer using the [get] method which takes a block for maximum safety.
     */
    fun <T: Any> getValue(key: Key<T>): T = this[key]!!

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
    fun <T : Any> putIfAbsent(key: Key<T>, provideInitialValue: () -> T, dispose: (T) -> Unit = {}, block: T.() -> Unit) {
        return lock.withLock {
            block((keyValues.computeIfAbsent(key) { Value(provideInitialValue(), dispose) } as Value<T>).wrapped)
        }
    }
}