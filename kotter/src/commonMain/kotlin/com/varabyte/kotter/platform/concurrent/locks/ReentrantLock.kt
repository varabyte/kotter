package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.internal.concurrent.*
import com.varabyte.kotter.platform.internal.concurrent.annotations.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

/**
 * A poor man's reimplementation of the JVM ReentrantLock class.
 *
 * Programmer's note: I am very nervous about this. This is an experiment to see if I can port my JVM library over to
 * multiplatform, and Kotter makes use of reentrant locks, which are otherwise not provided for the Kotlin/Native
 * target. Unfortunately, some Kotter APIs expose reentrant locks which means this class is now exposed to users.
 * Fortunately, it's not expected most users will ever interact with it, unless they are extending Kotter, which is
 * kind of advanced. For now, we'll experiment with this and see if it doesn't seem to cause any immediate concerns.
 */
@ThreadSafe
class ReentrantLock {
    private class Owner(val id: Any, var lockCount: Int = 1)

    private var currOwner: Owner? = null
    private val ownerMutex = Mutex()
    private val mainMutex = Mutex()

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
     * This is a fairly naive implementation so there is no concern for fairness or priority for who gets the lock when
     * there are multiple competitors.
     */
    fun lock() {
        val currThreadId = Thread.getId()

        runBlocking {
            val shouldAcquireLock: Boolean

            try {
                ownerMutex.lock()

                val currOwner = currOwner
                if (currOwner != null && currOwner.id == currThreadId) {
                    currOwner.lockCount++
                    return@runBlocking
                } else {
                    shouldAcquireLock = true
                }
            } finally {
                ownerMutex.unlock()
            }

            if (shouldAcquireLock) {
                while (true) {
                    ownerMutex.lock()

                    if (currOwner == null) {
                        currOwner = Owner(currThreadId)
                        ownerMutex.unlock()
                        mainMutex.lock()
                        break
                    } else {
                        ownerMutex.unlock()
                    }

                    Thread.sleepMs(0)
                }
            }
        }
    }

    /**
     * Release a hold required by [lock].
     *
     * There is no ownership checking here -- this code assumes that if you're calling unlock, it's because you
     * previously called lock and are now done with your work. This can be useful as you might start work on one thread
     * and finish it on another (for example, `delay` in coroutines might continue on a different thread in a pool).
     */
    fun unlock() {
        return runBlocking {
            try {
                ownerMutex.lock()
                val currOwner = currOwner ?: run {
                    return@runBlocking
                }

                currOwner.lockCount--

                if (currOwner.lockCount == 0) {
                    this@ReentrantLock.currOwner = null
                    mainMutex.unlock()
                } else {
                    return@runBlocking
                }
            } finally {
                ownerMutex.unlock()
            }
        }
    }
}

inline fun <T> ReentrantLock.withLock(block: () -> T): T {
    return try {
        lock()
        block()
    } finally {
        unlock()
    }
}
