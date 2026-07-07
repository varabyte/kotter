package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.internal.concurrent.Thread
import com.varabyte.kotter.platform.internal.concurrent.ThreadId
import com.varabyte.kotter.platform.internal.concurrent.annotations.ThreadSafe
import kotlinx.atomicfu.atomic
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
    /**
     * @property readerCounts A mapping of threads to how many read locks they are currently holding.
     * @property pausedReaderCounts Like [readerCounts], but they are temporarily paused while a thread has a write
     *   lock. When that write lock is released, relevant paused reader locks will be reactivated.
     * @property writerCount If non-null, indicates a thread has write access, and if multiple write locks are requested
     *   then the count is tracked here.
     */
    private class LockState(val readerCounts: Map<ThreadId, Int>, val pausedReaderCounts: Map<ThreadId, Int>, val writerCount: Pair<ThreadId, Int>?) {
        companion object {
            fun newForWriter(owner: ThreadId): LockState = LockState(readerCounts = emptyMap(), pausedReaderCounts = emptyMap(), writerCount = owner to 1)
            fun newForReader(owner: ThreadId): LockState = LockState(readerCounts = mapOf(owner to 1), pausedReaderCounts = emptyMap(), writerCount = null)
        }

        private fun Map<ThreadId, Int>.incrementCount(threadId: ThreadId): Map<ThreadId, Int> {
            val newCount = this[threadId]?.let { threadIdCount -> threadIdCount + 1 } ?: 1
            return this.toMutableMap().apply { this[threadId] = newCount }
        }

        private fun Map<ThreadId, Int>.decrementCount(threadId: ThreadId): Map<ThreadId, Int> {
            val newCount = this[threadId]?.let { it - 1 } ?: run {
                error("Thread [$threadId] attempted to release a read lock that it did not have.")
            }

            return this.toMutableMap().apply {
                if (newCount > 0) this[threadId] = newCount else this.remove(threadId)
            }
        }

        private fun moveCounts(threadId: ThreadId, from: Map<ThreadId, Int>, to: Map<ThreadId, Int>): Pair<Map<ThreadId, Int>, Map<ThreadId, Int>> {
            val fromCount = from[threadId] ?: return from to to
            check(!to.containsKey(threadId)) { "Thread [$threadId] already has counts in destination map." }

            val newFrom = from - threadId
            val newTo = to + (threadId to fromCount)
            return newFrom to newTo
        }

        private fun Pair<ThreadId, Int>?.incrementCount(threadId: ThreadId): Pair<ThreadId, Int> {
            if (this != null) {
                check(first == threadId) { "Thread [$threadId] attempted to grab write lock already held by another thread [$first]." }
                return first to second + 1
            } else {
                return threadId to 1
            }
        }
        private fun Pair<ThreadId, Int>.decrementCount(threadId: ThreadId): Pair<ThreadId, Int>? {
            check(first == threadId) { "Thread [$threadId] attempted to release write lock held by another thread [$first]."}
            return if (second > 1) first to second - 1 else null
        }

        fun addWriter(owner: ThreadId): LockState {
            val (newReaders, newPaused) = moveCounts(owner, readerCounts, pausedReaderCounts)
            return LockState(newReaders, newPaused, writerCount.incrementCount(owner))
        }
        fun addReader(owner: ThreadId) = LockState(readerCounts.incrementCount(owner), pausedReaderCounts, writerCount)
        fun addPausedReader(owner: ThreadId) = LockState(readerCounts, pausedReaderCounts.incrementCount(owner), writerCount)
        fun releaseWriter(owner: ThreadId): LockState? {
            check (writerCount != null) {
                "Thread [$owner] attempted to release write lock that nobody was holding."
            }
            val newWriterCount = writerCount.decrementCount(owner)

            return if (newWriterCount == null) {
                if (readerCounts.isEmpty() && pausedReaderCounts.isEmpty()) null else {
                    val (newPaused, newReaders) = moveCounts(
                        owner,
                        pausedReaderCounts,
                        readerCounts
                    )
                    LockState(newReaders, newPaused, newWriterCount)
                }
            } else {
                LockState(readerCounts, pausedReaderCounts, newWriterCount)
            }
        }
        fun releaseReader(owner: ThreadId): LockState? {
            val (newReaders, newPaused) = if (owner == writer) {
                readerCounts to pausedReaderCounts.decrementCount(owner)
            } else {
                readerCounts.decrementCount(owner) to pausedReaderCounts
            }

            return if (newReaders.isNotEmpty() || newPaused.isNotEmpty() || writerCount != null) {
                LockState(newReaders, newPaused, writerCount)
            } else null
        }

        fun pauseReader(owner: ThreadId): LockState {
            check (readerCounts.containsKey(owner) && !pausedReaderCounts.containsKey(owner))
            val (newReaders, newPaused) = moveCounts(owner, readerCounts, pausedReaderCounts)
            return LockState(newReaders, newPaused, writerCount)
        }

        val writer: ThreadId? get() = writerCount?.first
        fun isReader(owner: ThreadId) = readerCounts.containsKey(owner)
        fun canGrabWriteLock(owner: ThreadId) = writer == null && (readerCounts.isEmpty() || readerCounts.size == 1 && isReader(owner))
    }

    private val state = atomic<LockState?>(null)

    inner class ReaderLock {
        fun lock() {
            val currThread = Thread.getId()
            var spins = 0

            while (true) {
                val currState = state.value

                if (currState == null) {
                    // No one has the lock yet; try to grab it!
                    if (state.compareAndSet(currState, LockState.newForReader(currThread))) return
                } else {
                    if (currState.writer == null && currState.pausedReaderCounts.isEmpty()) {
                        // Lock already has readers -- so we can add more!
                        // But note, if there is a paused reader, that means a thread is in the process of securing
                        // write access, which we want to prioritize since it asked first
                        if (state.compareAndSet(currState, currState.addReader(currThread))) return
                    } else if (currState.writer == currThread) {
                        // This thread already has a write lock, so just note our read lock request and grant it (but
                        // paused for now; it will become a regular read lock after the write lock is released)
                        if (state.compareAndSet(currState, currState.addPausedReader(currThread))) return
                    }
                }

                // If here, someone else has the write lock or modified state before we did. Wait another loop!

                spins++
                yieldBasedOnSpinCount(spins)
            }
        }

        fun unlock() {
            val currThread = Thread.getId()
            var spins = 0

            while (true) {
                val currState = state.value
                    ?: error("Thread [$currThread] attempted to release read lock but no lock was acquired. Maybe you have mismatched lock / unlock calls?")

                if (state.compareAndSet(currState, currState.releaseReader(currThread))) return

                // If here, other threads modified state before we did. Wait another loop!

                spins++
                yieldBasedOnSpinCount(spins)
            }
        }
    }
    val reader = ReaderLock()

    inner class WriterLock {
        fun lock() {
            val currThread = Thread.getId()
            var spins = 0

            while (true) {
                val currState = state.value

                if (currState == null) {
                    // No one has the lock yet; try to grab it!
                    if (state.compareAndSet(currState, LockState.newForWriter(currThread))) return
                } else {
                    if (currState.writer == currThread || currState.canGrabWriteLock(currThread)) {
                        if (state.compareAndSet(currState, currState.addWriter(currThread))) return
                    } else if (currState.isReader(currThread)) {
                        // Another thread is holding a writer lock OR there is no writer but other readers. Temporarily
                        // recuse our reader locks and keep looping until we can grab a write lock.
                        if (state.compareAndSet(currState, currState.pauseReader(currThread))) continue
                    }
                }

                // If here, other threads are holding read locks or the writer lock or modified state before we did.
                // Wait another loop!

                spins++
                yieldBasedOnSpinCount(spins)
            }
        }

        fun unlock() {
            val currThread = Thread.getId()
            var spins = 0

            while (true) {
                val currState = state.value
                    ?: error("Thread [$currThread] attempted to release write lock but no lock was acquired. Maybe you have mismatched lock / unlock calls?")

                if (state.compareAndSet(currState, currState.releaseWriter(currThread))) return

                // If here, other threads modified state before we did. Wait another loop!

                spins++
                yieldBasedOnSpinCount(spins)
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
