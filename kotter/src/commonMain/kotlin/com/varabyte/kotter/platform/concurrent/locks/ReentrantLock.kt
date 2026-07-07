package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.internal.concurrent.Thread
import com.varabyte.kotter.platform.internal.concurrent.ThreadId
import com.varabyte.kotter.platform.internal.concurrent.annotations.ThreadSafe
import kotlinx.atomicfu.atomic
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// How many times to loop before falling back to a more cooperative behavior
private const val SPIN_THRESHOLD = 100

// Progressively back off a thread based on how many times it has spun in a loop
@Suppress("NOTHING_TO_INLINE")
internal inline fun yieldBasedOnSpinCount(spin: Int) {
    when {
        spin <= SPIN_THRESHOLD -> Thread.yield()
        spin <= (SPIN_THRESHOLD * 2) -> Thread.sleepMs(0)
        else -> Thread.sleepMs(1)
    }
}

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
    private class LockState(val owner: ThreadId, val holdCount: Int)
    private val state = atomic<LockState?>(null)

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
        var spins = 0

        while (true) {
            val currState = state.value

            // Lock is unowned? Quick, try to grab it!
            if (currState == null) {
                val newState = LockState(currThreadId, 1)
                if (state.compareAndSet(null, newState)) return
            } else {
                // If we already own the lock, we're done!
                if (currState.owner == currThreadId) {
                    val newState = LockState(currThreadId, currState.holdCount + 1)
                    if (state.compareAndSet(currState, newState)) return
                }
            }

            // Someone else owns the lock -- backoff progressively
            spins++
            yieldBasedOnSpinCount(spins)
        }
    }

    /**
     * Release a hold required by [lock].
     *
     * It is an error to try to call this method if you didn't previously call [lock].
     */
    fun unlock() {
        val currThreadId = Thread.getId()

        while (true) {
            val currState = state.value
            val currOwner = currState?.owner

            if (currOwner != currThreadId) {
                error(buildString {
                    append("Thread [$currThreadId] attempted to unlock a lock ")
                    if (currState != null) {
                        append("owned by Thread [$currOwner].")
                    } else {
                        append("that was never locked.")
                    }
                })
            }

            val newState = if (currState.holdCount > 1) {
                LockState(currThreadId, currState.holdCount - 1)
            } else null

            // Shouldn't happen in practice -- only the owning thread can unlock a lock, so we shouldn't expect that
            // state got changed by a different thread, but this pattern seems to feel safer with atomic writes.
            if (state.compareAndSet(currState, newState)) return
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