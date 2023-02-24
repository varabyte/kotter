package com.varabyte.kotter.platform.concurrent

import kotlin.native.concurrent.AtomicReference as NativeAtomicReference

internal actual class AtomicReference<T> actual constructor(initialValue: T) {
    private val delegate = NativeAtomicReference(initialValue)
    actual val value: T get() = delegate.value

    actual fun compareAndSet(expected: T, newValue: T) = delegate.compareAndSet(expected, newValue)
}