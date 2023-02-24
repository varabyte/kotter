package com.varabyte.kotter.platform.concurrent

import net.jcip.annotations.ThreadSafe
import java.util.concurrent.atomic.AtomicReference as JvmAtomicReference

@ThreadSafe
internal actual class AtomicReference<T> actual constructor(initialValue: T) {
    private val delegate = JvmAtomicReference(initialValue)
    actual val value: T get() = delegate.get()

    actual fun compareAndSet(expected: T, newValue: T) = delegate.compareAndSet(expected, newValue)
}

fun deleteme() {
    val test = mutableMapOf<Int, Int>()
    test.computeIfAbsent(20) { it * 2 }
}