package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.concurrent.locks.internal.ProgressiveBackoffYielder
import com.varabyte.kotter.platform.concurrent.locks.internal.SpinLock
import com.varabyte.kotter.platform.internal.concurrent.Thread
import com.varabyte.kotter.platform.internal.concurrent.ThreadId
import com.varabyte.kotter.platform.internal.concurrent.annotations.ThreadSafe
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A poor man's reimplementation of the JVM ReentrantLock class.
 *
 * Programmer's note: This class was introduced when I converted Kotter from JVM-only to KMP (since reentrant locks
 * are not provided for the Kotlin/Native target). I wanted to see if it was possible to have an algorithm working in
 * commonMain instead of delegating to expect/actual, but it is a naive implementation, so we can revisit this
 * decision later.
 */
@ThreadSafe
class ReentrantLock {
    private val spinLock = SpinLock()

    private var ownerThread: ThreadId? = null
    private var holdCount = 0

    /**
     * Request a lock.
     *
     * This will succeed immediately if:
     * * it's the first call in line to request a lock
     * * this class is already locked but by the same thread (implying this is a re-entrant call)
     *
     * Otherwise, it will block until some point in the future where it can acquire the lock, after the previous lock
     * holders have released it.
     *
     * This is a fairly naive implementation, so there is no concern for fairness or priority for who gets the lock when
     * there are multiple competitors.
     */
    fun lock() {
        val currThreadId = Thread.getId()
        var yielder: ProgressiveBackoffYielder? = null

        while (true) {
            spinLock.withLock {
                if (ownerThread == null) {
                    ownerThread = currThreadId
                    holdCount = 1
                    return
                } else if (ownerThread == currThreadId) {
                    holdCount++
                    return
                }
            }

            yielder = yielder ?: ProgressiveBackoffYielder()
            yielder.yield()
        }
    }

    /**
     * Release a hold required by [lock].
     *
     * It is an error to try to call this method if you didn't previously call [lock].
     */
    fun unlock() {
        val currThreadId = Thread.getId()
        spinLock.withLock {
            if (ownerThread != currThreadId) {
                error("Thread [$currThreadId] attempted to unlock a lock it does not own.")
            }

            holdCount--
            if (holdCount == 0) {
                ownerThread = null
            }
        }
    }
}

inline fun <T> ReentrantLock.withLock(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}