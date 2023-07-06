package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.internal.concurrent.*
import com.varabyte.kotter.platform.internal.concurrent.annotations.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
 * Programmer's note: See note in ReentrantLock about how bad an idea this probably is...
 */
@ThreadSafe
class ReentrantReadWriteLock {
    private enum class State {
        IDLE,
        READING,
        WRITING,
    }

    private val stateLock = Mutex()
    private var writeCount = 0
    private var currState = State.IDLE
    private var readingThreadsIds = mutableListOf<ThreadId>()
    private var readCountsToRestore = mutableMapOf<ThreadId, Int>()
    private var writingThreadId: ThreadId? = null

    inner class ReaderLock {
        fun lock() = runBlocking {
            val threadId = Thread.getId()
            var waitInLine = true
            while (waitInLine) {
                stateLock.withLock {
                    if (currState == State.IDLE) {
                        currState = State.READING
                    }

                    // Note: If you own the write lock, you can read if you want to! You're the boss!
                    if (currState == State.READING || (currState == State.WRITING && writingThreadId == threadId)) {
                        readingThreadsIds.add(threadId)
                        waitInLine = false
                    }
                }

                if (waitInLine) Thread.sleepMs(0)
            }
        }

        fun unlock() = runBlocking {
            val threadId = Thread.getId()
            stateLock.withLock {
                readingThreadsIds.remove(threadId)
                if (readingThreadsIds.isEmpty() && writeCount == 0) {
                    currState = State.IDLE
                }
            }
        }
    }

    val readerLock = ReaderLock()

    inner class WriterLock {
        fun lock() = runBlocking {
            var waitInLine = true
            val threadId = Thread.getId()

            while (waitInLine) {
                stateLock.withLock {
                    if (currState == State.READING && !readCountsToRestore.containsKey(threadId)) {
                        val readCountsToRelease = readingThreadsIds.count { it == threadId }
                        if (readCountsToRelease > 0) {
                            readingThreadsIds.removeAll { it == threadId }
                            readCountsToRestore[threadId] = readCountsToRelease
                        }
                        if (readingThreadsIds.isEmpty()) {
                            currState = State.IDLE
                        }
                    }

                    if (currState == State.IDLE) {
                        currState = State.WRITING
                        writingThreadId = threadId
                    }

                    if ((currState == State.WRITING && writingThreadId == threadId)) {
                        writeCount++
                        waitInLine = false
                    }
                }

                if (waitInLine) Thread.sleepMs(0)
            }
        }

        fun unlock() = runBlocking {
            val threadId = Thread.getId()

            stateLock.withLock {
                writeCount--
                if (writeCount == 0) {
                    val readCountsToAcquire = readCountsToRestore.remove(threadId) ?: 0
                    repeat(readCountsToAcquire) { readingThreadsIds.add(threadId) }

                    currState = if (readingThreadsIds.isEmpty()) State.IDLE else State.READING
                }
            }

        }
    }

    val writerLock = WriterLock()
}

inline fun <T> ReentrantReadWriteLock.read(block: () -> T): T {
    return try {
        readerLock.lock()
        block()
    } finally {
        readerLock.unlock()
    }
}

inline fun <T> ReentrantReadWriteLock.write(block: () -> T): T {
    return try {
        writerLock.lock()
        block()
    } finally {
        writerLock.unlock()
    }
}
