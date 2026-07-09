package com.varabyte.kotter.platform.concurrent.locks.internal

import com.varabyte.kotter.platform.internal.concurrent.Thread
import com.varabyte.kotter.platform.internal.concurrent.annotations.ThreadSafe
import kotlinx.atomicfu.atomic

/**
 * Briefly yield this thread to allow other threads to run.
 *
 * The more often [yield] is called, the more it will back off.
 *
 * To avoid unnecessary allocations when threads are in a normal, hopefully non-contentious state, consider allocating
 * lazily:
 * ```
 * var yielder: ProgressiveBackoffYielder? = null
 * while (true) {
 *    yielder = yielder ?: ProgressiveBackoffYielder()
 *    yielder.yield()
 * }
 * ```
 */
internal class ProgressiveBackoffYielder {
    companion object {
        // How many times to loop before falling back to a more cooperative behavior
        private const val SPIN_THRESHOLD = 100
    }

    private var spins: Int = 0

    @Suppress("NOTHING_TO_INLINE")
    inline fun yield() {
        when {
            spins <= SPIN_THRESHOLD -> Thread.yield()
            spins <= (SPIN_THRESHOLD * 2) -> Thread.sleepMs(0)
            else -> Thread.sleepMs(1)
        }
        ++spins
    }
}

/**
 * A simple lock class that uses a spinning while loop with progressive back off as a way to make sure only one caller
 * can run at a time.
 */
@ThreadSafe
internal class SpinLock {
    private val isHeld = atomic(false)
    inline fun <T> withLock(block: () -> T): T {
        var yielder: ProgressiveBackoffYielder? = null
        while (!isHeld.compareAndSet(expect = false, update = true)) {
            yielder = yielder ?: ProgressiveBackoffYielder()
            yielder.yield()
        }
        return try {
            block()
        } finally {
            isHeld.value = false
        }
    }
}
