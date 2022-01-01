package com.varabyte.kotter.runtime.concurrent

import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData.Key
import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData.Lifecycle
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A thread-safe package of key/value pairs.
 *
 * You should register data using a predefined [Key] object. When registering values, an optional dispose block can be
 * specified at the same time, which will automatically be triggered when the block is done running.
 *
 * This data will be tied to a [Lifecycle]. You must call [start] with a lifecycle first before keys tied to that
 * lifecycle can be added. Afterwards, call [stop] to remove all keys with the matching lifecycle and deactivate it.
 *
 * Note: Although the class is designed to be thread safe, some calls (which are noted with warnings) can expose values
 * to you which are no longer covered by the lock. Those should only be used to fetch immutable or thread-safe values.
 */
@ThreadSafe
@Suppress("UNCHECKED_CAST")
class ConcurrentScopedData {
    /**
     * A marker interface for an object that represents a class's lifecycle. For example, you might declare something
     * like so:
     *
     * ```
     * class Application {
     *    object Lifecycle : ConcurrentScopedData.Lifecycle
     * }
     * ```
     *
     * and then, if you ever create a new key for some unique data you want to add, you can write:
     *
     * ```
     * object CredentialsKey : ConcurrentScopedData.Key<String> {
     *   override val lifecycle = Application.Lifecycle
     * }
     *
     * data[CredentialsKey] = UUID.randomUUID().toString()
     * ```
     *
     * For convenience, you can also create keys with the following shorthand syntax:
     * ```
     * val CredentialsKey = Application.Lifecycle.createKey<String>()
     * ```
     *
     * Finally, you can associate one lifecycle as a child of another, so that if the parent lifecycle is stopped,
     * this one will get stopped as well too:
     *
     * ```
     * // You can either stop this dialog's lifecycle directly, or it will get stopped when the overall UI is stopped.
     * class Dialog {
     *   object Lifecycle : ConcurrentScopedData.Lifecycle {
     *     override val parent = Ui.Lifecycle
     *   }
     * }
     * ```
     */
    interface Lifecycle {
        val parent: Lifecycle? get() = null
    }
    interface Key<T> {
        val lifecycle: Lifecycle
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

    /**
     * The lock used by this data, which however is exposed so that it can be shared by other threaded logic, in order
     * to eliminate possible deadlocks caused by multiple locks being held at the same time.
     */
    val lock = ReentrantReadWriteLock()
    @GuardedBy("lock")
    private val activeLifecycles = mutableSetOf<Lifecycle>()
    @GuardedBy("lock")
    private val keyValues = mutableMapOf<Key<out Any>, Value<out Any>>()

    /**
     * Start a lifecycle (if not already active).
     *
     * Any keys that are added (e.g. with [tryPut] and [putIfAbsent]) when a lifecycle is not active will be silently
     * ignored.
     */
    fun start(lifecycle: Lifecycle) {
        lock.write {
            activeLifecycles.add(lifecycle)
        }
    }

    /**
     * Dispose and remove all keys tied to the specified [Lifecycle]
     */
    fun stop(lifecycle: Lifecycle) {
        lock.write {
            if (activeLifecycles.remove(lifecycle)) {
                keyValues.entries.removeAll { entry ->
                    if (entry.key.lifecycle === lifecycle) {
                        entry.value.dispose()
                        true
                    } else {
                        false
                    }
                }
            }

            activeLifecycles.filter { it.parent === lifecycle }.forEach { stop(it) }
        }
    }

    /**
     * Dispose and remove ALL keys in this data store.
     *
     * Of course, [stop] should be preferred, but this can be useful if handling an unrecoverable exception, for
     * example, or if we're shutting down the whole session.
     */
    fun stopAll() {
        lock.write {
            // Make a copy of the list to avoid concurrent modification exception
            activeLifecycles.toList().forEach { stop(it) }
        }
    }

    fun isActive(lifecycle: Lifecycle) = lock.read { activeLifecycles.contains(lifecycle) }

    /**
     * Access the stored value directly.
     *
     * Warning: Be careful with this method, as you are accessing data from outside a lock it is normally stored behind.
     * Prefer using the [get] method which takes a block for maximum safety.
     */
    operator fun <T : Any> get(key: Key<T>): T? {
        return lock.read { (keyValues[key] as? Value<T>)?.wrapped }
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
        lock.read { this[key]?.let { value -> value.block() } }
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
        return lock.write { keyValues[key] = Value(value, dispose) }
    }

    /**
     * Attempt to remove a key directly, which will trigger its dispose call if present.
     *
     * Returns true if the key was removed and disposed, false otherwise.
     */
    fun <T : Any> remove(key: Key<T>): Boolean {
        return lock.write {
            val value = keyValues.remove(key)?.also { it.dispose() }
            value != null
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
     * Returns true if the value was added, false if something already existed with the associated key, or if the
     * lifecycle associated for the current key hasn't yet been activated with [start].
     */
    fun <T : Any> tryPut(key: Key<T>, provideInitialValue: () -> T, dispose: (T) -> Unit): Boolean {
        return lock.read {
            var wasPut = false
            if (isActive(key.lifecycle)) {
                lock.write {
                    keyValues.computeIfAbsent(key) { wasPut = true; Value(provideInitialValue(), dispose) }
                }
            }
            wasPut
        }
    }

    /**
     * Run the target [block] with the current value inside this data store, or if one doesn't exist, trigger
     * [provideInitialValue] to generate it. This ensured that there's always a value for [block] to be called with. An
     * optional [dispose] block can be provided.
     *
     * Note that this fails silently if the key being added is for a lifecycle that hasn't yet been activated with
     * [start].
     */
    fun <T : Any> putIfAbsent(
        key: Key<T>,
        provideInitialValue: () -> T,
        dispose: (T) -> Unit = {},
        block: T.() -> Unit
    ) {
        return lock.read {
            if (isActive(key.lifecycle)) {
                lock.write {
                    // Add manually instead of using computeIfAbsent, as `provideInitialValue()` may itself
                    // register additional keys, which is legal but we don't want it to cause a
                    // ConcurrentModificationException
                    if (!keyValues.containsKey(key)) {
                        keyValues[key] = Value(provideInitialValue(), dispose)
                    }

                    block((keyValues.getValue(key) as Value<T>).wrapped)
                }
            }
        }
    }
}

/**
 * A convenience method for creating a key when you already have the lifecycle.
 *
 * This often saves a few lines of code and is easier to type.
 */
fun <T> Lifecycle.createKey(): Key<T> {
    val self = this
    return object : ConcurrentScopedData.Key<T> {
        override val lifecycle = self
    }
}