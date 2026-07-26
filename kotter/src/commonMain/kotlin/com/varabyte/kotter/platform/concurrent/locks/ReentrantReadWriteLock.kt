package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.concurrent.locks.internal.ProgressiveBackoffYielder
import com.varabyte.kotter.platform.concurrent.locks.internal.SpinLock
import com.varabyte.kotter.platform.internal.concurrent.Thread
import com.varabyte.kotter.platform.internal.concurrent.ThreadId
import com.varabyte.kotter.platform.internal.concurrent.annotations.ThreadSafe
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A poor man's reimplementation of the JVM ReentrantReadWriteLock class.
 *
 * The basic idea is that you can run any number of read requests concurrently across any number of threads, OR you can
 * run a single write request, during which time other writes and even read requests will block until it is released.
 *
 * There's one exception to the write lock limitation: because this class is reentrant, if you own a write lock, and
 * then request an additional write lock, it will work. (This might happen if you are in one method that has write
 * access which calls another method that requests write access)
 *
 * Additionally, threads that own a write lock can also request a read lock (which might seem like a strange thing to do
 * but could occur in practice if a method that has write access calls another method that requests read access).
 *
 * Finally, if you are the *only* thread making a read request *and* you request write access, then your access will
 * temporarily be upgraded to write mode. (Of course, if other reads were open, the write request would temporarily
 * block until all other readers relinquished their read access)
 *
 * Programmer's note: This class was introduced when I converted Kotter from JVM-only to KMP (since reentrant locks
 * are not provided for the Kotlin/Native target). I wanted to see if it was possible to have an algorithm working in
 * commonMain instead of delegating to expect/actual, but it is a naive implementation, so we can revisit this
 * decision later.
 */
@ThreadSafe
class ReentrantReadWriteLock {
    private val spinLock = SpinLock()

    private val readerCounts = mutableMapOf<ThreadId, Int>()
    // When a thread that already has read locks asks for a write lock, it pauses the read locks; they will get
    // restored after the write lock is released.
    private val pausedReaderThreads = mutableSetOf<ThreadId>()
    private var writerThread: ThreadId? = null
    private var writerCount = 0

    inner class ReaderLock {
        fun lock() {
            val currThread = Thread.getId()
            var yielder: ProgressiveBackoffYielder? = null

            while (true) {
                spinLock.withLock {
                    if (writerThread == null) {
                        // If there's no writer at this point, we still want to play nice if one or more threads is
                        // waiting in line to be one. In that case, we can spin for a little longer, giving it a chance
                        // to run and finish.
                        //
                        // One exception is if we are an inner read inside an already running outer read! In that case,
                        // if we try to be polite, then we'll wait for the write lock to start, and the write lock will
                        // wait for our parent read that will never finish.
                        val isNestedRead = readerCounts.containsKey(currThread)
                        val hasPausedReaders = pausedReaderThreads.isNotEmpty()

                        if (isNestedRead || !hasPausedReaders) {
                            readerCounts[currThread] = (readerCounts[currThread] ?: 0) + 1
                            return
                        }
                    }

                    if (writerThread == currThread) {
                        // This thread holds the write lock; queue this as a paused read
                        pausedReaderThreads.add(currThread)
                        readerCounts[currThread] = (readerCounts[currThread] ?: 0) + 1
                        return
                    }
                }

                yielder = yielder ?: ProgressiveBackoffYielder()
                yielder.yield()
            }
        }

        fun unlock() {
            val currThread = Thread.getId()
            spinLock.withLock {
                val count = readerCounts[currThread] ?: error("Mismatched lock/unlock")
                if (count > 1) readerCounts[currThread] = count - 1 else {
                    readerCounts.remove(currThread)
                    pausedReaderThreads.remove(currThread)
                }
            }
        }
    }
    val reader = ReaderLock()

    inner class WriterLock {
        fun lock() {
            val currThread = Thread.getId()
            var yielder: ProgressiveBackoffYielder? = null

            while (true) {
                spinLock.withLock {
                    if (writerThread == currThread) {
                        writerCount++
                        return
                    }

                    if (writerThread == null) {
                        // If we have read locks, declare intent to write by pausing them.
                        if (readerCounts.containsKey(currThread)) {
                            pausedReaderThreads.add(currThread)
                        }

                        // We cannot grab a write lock until ALL readers are either paused or released
                        val activeReadersCount = readerCounts.keys.count { it !in pausedReaderThreads }
                        if (activeReadersCount == 0) {
                            writerThread = currThread
                            writerCount = 1
                            return
                        }
                    }
                }

                yielder = yielder ?: ProgressiveBackoffYielder()
                yielder.yield()
            }
        }

        fun unlock() {
            val currThread = Thread.getId()
            spinLock.withLock {
                check(writerThread == currThread) { "Thread [$currThread] does not hold write lock." }
                writerCount--

                if (writerCount == 0) {
                    writerThread = null
                    // Restore paused readers back to active readers
                    pausedReaderThreads.remove(currThread)
                }
            }
        }
    }
    val writer = WriterLock()
}

inline fun <T> ReentrantReadWriteLock.read(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    reader.lock()
    return try {
        block()
    } finally {
        reader.unlock()
    }
}

inline fun <T> ReentrantReadWriteLock.write(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    writer.lock()
    return try {
        block()
    } finally {
        writer.unlock()
    }
}
