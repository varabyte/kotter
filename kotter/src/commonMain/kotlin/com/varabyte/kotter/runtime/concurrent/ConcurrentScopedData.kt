package com.varabyte.kotter.runtime.concurrent

import com.varabyte.kotter.platform.collections.computeIfAbsent
import com.varabyte.kotter.platform.collections.removeIf
import com.varabyte.kotter.platform.concurrent.annotations.GuardedBy
import com.varabyte.kotter.platform.concurrent.annotations.ThreadSafe
import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData.Key
import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData.Lifecycle
import com.varabyte.kotter.runtime.concurrent.locks.ReentrantReadWriteLock

/**
 * A thread-safe collection of key/value pairs, where additionally each key is typed and tied to a [Lifecycle].
 *
 * You register values against a typed [Key] object. When registering them, an optional dispose block can be specified
 * as well, which will automatically be triggered whenever the key is removed (either manually or because its lifecycle
 * ended).
 *
 * You must call [start] with a [Lifecycle] first before keys tied to it can be added. Afterwards, call [stop] to remove
 * all keys with the matching lifecycle.
 *
 * For example:
 *
 * ```
 * // Globals somewhere
 * object MyLifecycle : ConcurrentScopedData.Lifecycle
 * val MyKey = MyLifecycle.createKey<Boolean>()
 *
 * // Later...
 * data = ConcurrentScopedData()
 * data.start(MyLifecycle)
 * data[MyKey] = true
 * ...
 * data.stop(MyLifecycle) // MyKey is removed
 * ```
 *
 * Note: Although the class is designed to be thread safe, some calls (which are noted with warnings) can expose values
 * to you which are no longer covered by the lock. Those should only be used to fetch immutable or thread-safe values.
 */
@ThreadSafe
@Suppress("UNCHECKED_CAST")
class ConcurrentScopedData {
    /**
     * A marker interface for an object that represents a some lifetime.
     *
     * For example, you might declare a lifecycle tied to its owning class like so:
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
     * val CredentialsKey = Application.Lifecycle.createKey<String>()
     * assertTrue(data.isActive(Application.Lifecycle)) // Must be true before adding data
     * data[CredentialsKey] = UUID.randomUUID().toString()
     * ```
     *
     * You can associate one lifecycle as the child of another, so that if the parent lifecycle is stopped, this child
     * lifecycle will get stopped as well too:
     *
     * ```
     * class Ui {
     *   object Lifecycle : ConcurrentScopedData.Lifecycle
     * }
     *
     * class Dialog {
     *   object Lifecycle : ConcurrentScopedData.Lifecycle {
     *     override val parent = Ui.Lifecycle
     *   }
     * }
     *
     * data.start(Ui.Lifecycle)
     * data.start(Dialog.Lifecycle)
     * data[SomeDialogKey] = 12345
     * data.stop(Ui.Lifecycle) // Removes all UI keys and dialog keys, too!
     * ```
     *
     * See also: [start], [stop], [isActive]
     */
    interface Lifecycle {
        val parent: Lifecycle? get() = null
    }

    /**
     * A tag for typed data tied to a [Lifecycle]
     *
     * You are encouraged to use [Lifecycle.createKey] as it's easier to type, but you can manually create a key and use
     * it like so:
     *
     * ```
     * object DemoKey : ConcurrentScopedData.Key<Int> {
     *   override val lifecycle = SomeLifecycle
     * }
     *
     * ... later ...
     * data.start(SomeLifecycle)
     * data[DemoKey] = 123
     * ```
     */
    interface Key<T> {
        val lifecycle: Lifecycle
    }

    /**
     * Fields provided to the [onLifecycleDeactivated] callback.
     *
     * @property lifecycle The lifecycle that got deactivated.
     * @property removeListener Set it to true to remove this callback after it got triggered.
     */
    class LifecycleListenerScope(val lifecycle: Lifecycle, var removeListener: Boolean = false)

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
     * The lock used by this data.
     *
     * It is exposed so that other code can *carefully* tie some of their own logic to the same lock - for example,
     * waiting until they can be sure that they have write access to this data before running some preemptive logic to
     * generate some value that they want to put into it.
     */
    val lock = ReentrantReadWriteLock()
    @GuardedBy("lock")
    private val activeLifecycles = mutableSetOf<Lifecycle>()
    @GuardedBy("lock")
    private val keyValues = mutableMapOf<Key<out Any>, Value<out Any>>()

    @GuardedBy("lock")
    private val onLifecycleDeactivated = mutableListOf<LifecycleListenerScope.() -> Unit>()

    /**
     * Add a listener that will be triggered whenever a lifecycle goes from active -> inactive state.
     *
     * See also [LifecycleListenerScope] which contains information about which lifecycle was deactivated.
     */
    fun onLifecycleDeactivated(listener: LifecycleListenerScope.() -> Unit) {
        lock.write {
            onLifecycleDeactivated.add(listener)
        }
    }

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
            onLifecycleDeactivated.removeIf { event ->
                val scope = LifecycleListenerScope(lifecycle)
                scope.event()
                scope.removeListener
            }
        }
    }

    /**
     * Stop all lifecycles, thereby disposing and removing all keys as a side effect.
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

    /** Returns true if the target lifecycle has been started (and not yet stopped). */
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

    /**
     * Check if a key currently exists in this data store.
     */
    fun <T : Any> contains(key: Key<T>): Boolean {
        return lock.read { keyValues.contains(key) }
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
     * A simple remove option which returns true if the value was removed or not. If removed, its dispose call would
     * have been triggered.
     */
    fun <T : Any> remove(key: Key<T>): Boolean {
        var removed = false
        remove(key) { removed = true }
        return removed
    }

    /**
     * Attempt to remove a key directly, which will trigger its dispose call if present. If a value was removed, it
     * will additionally be triggered in the passed-in [block].
     */
    fun <T : Any> remove(key: Key<T>, block: T.() -> Unit) {
        lock.write {
            val value = (keyValues.remove(key) as? Value<T>)?.also {
                it.wrapped.block()
                it.dispose()
            }
            value != null
        }
    }

    /**
     * Put an item into the data store if one is not already in place, then return whatever was associated with the key.
     *
     * If you plan to immediately modify the value returned by this method, you should prefer calling [putIfAbsent] over
     * this one, as it will trigger code within the same write lock that inserts the element into this data store,
     * meaning you can be sure that you're the first person to modify its value in that case.
     *
     * Note that this method can return null if the lifecycle associated with the current key is not started yet!
     */
    fun <T : Any> putOrGet(key: Key<T>, provideInitialValue: () -> T, dispose: (T) -> Unit): T? {
        var result: T? = null
        putIfAbsent(key, provideInitialValue, dispose) {
            result = this
        }
        return result
    }

    /**
     * A convenience version of the other [putOrGet] call but without a dispose block, making the trailing-lambda syntax for
     * this common case nicer.
     */
    fun <T : Any> putOrGet(key: Key<T>, provideInitialValue: () -> T): T? {
        return putOrGet(key, provideInitialValue, dispose = {})
    }

    /**
     * A convenience version of the other [tryPut] call but without a dispose block, making the trailing-lambda syntax for
     * this common case nicer.
     */
    fun <T : Any> tryPut(key: Key<T>, provideInitialValue: () -> T): Boolean {
        return tryPut(key, provideInitialValue, dispose = {})
    }

    /**
     * Like [putIfAbsent], but returns a boolean instead of triggering a block.
     *
     * Returns true if the value was added, false if something already existed with the associated key or if the
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
                    // Add manually instead of using computeIfAbsent, as `provideInitialValue()` may itself register
                    // additional keys, which is legal, but we don't want it to cause a ConcurrentModificationException
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
    return object : Key<T> {
        override val lifecycle = self
    }
}