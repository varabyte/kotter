package com.varabyte.kotter.platform.internal.concurrent

internal expect class AtomicReference<T>(initialValue: T) {
    val value: T

    fun compareAndSet(expected: T, newValue: T): Boolean
}