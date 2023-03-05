package com.varabyte.kotter.platform.internal.concurrent

import net.jcip.annotations.ThreadSafe
import java.util.concurrent.atomic.AtomicReference as JvmAtomicReference

@ThreadSafe
internal actual class AtomicReference<T> actual constructor(initialValue: T) {
    private val delegate = JvmAtomicReference(initialValue)
    actual val value: T get() = delegate.get()

    actual fun compareAndSet(expected: T, newValue: T) = delegate.compareAndSet(expected, newValue)
}
