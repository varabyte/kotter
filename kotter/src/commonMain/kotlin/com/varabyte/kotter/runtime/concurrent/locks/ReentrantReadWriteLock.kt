package com.varabyte.kotter.runtime.concurrent.locks

import com.varabyte.kotter.platform.concurrent.Thread
import com.varabyte.kotter.platform.concurrent.annotations.ThreadSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A poor man's reimplementation of the JVM ReentrantReadWriteLock class.
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
    private var readingThreadsIds = mutableListOf<Any>()
    private var writingThreadId: Any? = null

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

                if (waitInLine) delay(0)
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
                    // Special case -- if you're the only reader, then we can promote to write
                    if (currState == State.IDLE || currState == State.READING && readingThreadsIds.all { it == threadId }) {
                        currState = State.WRITING
                        writingThreadId = threadId
                    }

                    if ((currState == State.WRITING && writingThreadId == threadId)) {
                        writeCount++
                        waitInLine = false
                    }
                }

                if (waitInLine) delay(0)
            }
        }

        fun unlock() = runBlocking {
            stateLock.withLock {
                writeCount--
                if (writeCount == 0) {
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
