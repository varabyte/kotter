package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.internal.concurrent.Thread
import com.varabyte.truthish.assertThat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// NOTE: If this test seems sparse for such potentially delicate code, it's because the general usage of the lock class
// is already tested by the rest of the tests indirectly. However, as for now (this may change later), this class exists
// to test really gnarly edge cases I want to make sure are working
class ReentrantReadWriteLockTest {
    @Test
    fun multipleConcurrentReadersAllowed() = runTest {
        // Use real threads and not fake coroutine threads because lock / unlock uses real thread IDs under the hood to
        // manage who owns any active read locks or write lock.
        val testDispatcher = Dispatchers.Default.limitedParallelism(3)

        val lock = ReentrantReadWriteLock()

        // Channels to coordinate the exact overlapping of readers
        val readerEntered = Channel<Unit>(capacity = 3)
        val releaseReaders = CompletableDeferred<Unit>()
        val finishedCount = Channel<Unit>(capacity = 3)

        // Spin up 3 concurrent readers
        val jobs = List(3) {
            launch(testDispatcher) {
                lock.read {
                    // Signal that this reader has successfully entered the lock
                    readerEntered.send(Unit)

                    // Wait here until ALL readers have entered
                    releaseReaders.await()
                }
                finishedCount.send(Unit)
            }
        }

        // Wait until all 3 readers are simultaneously inside the read lock
        repeat(3) { readerEntered.receive() }

        // If the lock wasn't concurrent, we would deadlock here because the first
        // reader would never exit to let the next one enter.
        // Since we reached this line, concurrency is proven. Release them!
        releaseReaders.complete(Unit)

        // Ensure all of them finish cleanly
        repeat(3) { finishedCount.receive() }
        jobs.joinAll()
    }

    enum class WriteLockState {
        NOT_STARTED,
        ENTERED,
        FINISHING,
        FINISHED,
    }

    @Test
    fun writerExcludesReaders() = runTest {
        // Use real threads and not fake coroutine threads because lock / unlock uses real thread IDs under the hood to
        // manage who owns any active read locks or write lock.
        val testDispatcher = Dispatchers.Default.limitedParallelism(2)

        val lock = ReentrantReadWriteLock()

        val writerInside = CompletableDeferred<Unit>()
        val readerFinished = CompletableDeferred<Unit>()
        val writerFinished = CompletableDeferred<Unit>()

        var writeLockState = WriteLockState.NOT_STARTED
        var readerObservedState: WriteLockState? = null

        // Thread 1: Claims the write lock
        launch(testDispatcher) {
            lock.write {
                writeLockState = WriteLockState.ENTERED
                writerInside.complete(Unit)

                // Artificially suspend while holding the lock until the reader
                // has attempted to run and block
                yield()
                writeLockState = WriteLockState.FINISHING
            }
            readerFinished.await()
            writerFinished.complete(Unit)
            writeLockState = WriteLockState.FINISHED
        }

        // Thread 2: Attempts to read
        launch(testDispatcher) {
            // Wait until we are absolutely certain the writer is inside its block
            writerInside.await()

            lock.read {
                readerObservedState = writeLockState
            }
            readerFinished.complete(Unit)
        }

        // If the write lock is exclusive, the reader cannot finish before the writer exits
        writerFinished.await() // Implies readerFinished.await() already happened

        // The reader must have only seen the value *after* the writer completed entirely
        assertThat(readerObservedState).isEqualTo(WriteLockState.FINISHING)
    }

    @Test
    fun readLockIsReentrantOnSameThread() {
        val currThreadId = Thread.getId()
        val lock = ReentrantReadWriteLock()
        var executed = false

        lock.read {
            assertThat(Thread.getId()).isEqualTo(currThreadId)
            lock.read {
                assertThat(Thread.getId()).isEqualTo(currThreadId)
                lock.read {
                    assertThat(Thread.getId()).isEqualTo(currThreadId)
                    executed = true
                }
            }
        }

        assertThat(executed).isTrue()
    }

    @Test
    fun writeLockIsReentrantOnSameThread() {
        val currThreadId = Thread.getId()
        val lock = ReentrantReadWriteLock()
        var executed = false

        lock.write {
            assertThat(Thread.getId()).isEqualTo(currThreadId)
            lock.write {
                assertThat(Thread.getId()).isEqualTo(currThreadId)
                lock.write {
                    executed = true
                }
            }
        }

        assertThat(executed).isTrue()
    }

    @Test
    fun writeLockCanAcquireReadLockOnSameThread() {
        val currThreadId = Thread.getId()
        val lock = ReentrantReadWriteLock()
        var executed = false

        lock.write {
            assertThat(Thread.getId()).isEqualTo(currThreadId)
            // A writer should always be allowed to acquire a read lock
            lock.read {
                assertThat(Thread.getId()).isEqualTo(currThreadId)
                // A reader should be able to repromote back to a write lock because they're still inside it
                lock.write {
                    assertThat(Thread.getId()).isEqualTo(currThreadId)
                    lock.read {
                        assertThat(Thread.getId()).isEqualTo(currThreadId)
                        executed = true
                    }
                }
            }
        }

        assertThat(executed).isTrue()
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun twoThreadsCanConvertReadToWriteLocksAtTheSameTime() = runTest {
        // If two threads both do this:
        //   lock.read {
        //      lock.write { }
        //   }
        // and if the lock implementation for promoting a read lock to a write lock is bad, we can end up with a
        // deadlock, as both blocks wait for the read count to go to 0 before acquiring the lock. The way the lock class
        // actually works is anytime a write is requested, all reads are temporarily released (only to be reacquired
        // after the write is done). In this way, between the two threads, one of them will be ready to write when both
        // locks have released all reads.
        //
        // After the first thread finishes its write, it will then be back in read mode, just for a moment before it
        // releases it. And then the second thread will finally get a go.

        val dispatcher1 = newFixedThreadPoolContext(1, "Thread 1")
        val dispatcher2 = newFixedThreadPoolContext(1, "Thread 2")

        try {
            val lock = ReentrantReadWriteLock()
            val thread1InReadMode = CompletableDeferred<Unit>()
            val thread2InReadMode = CompletableDeferred<Unit>()
            val thread1InWriteMode = CompletableDeferred<Unit>()
            val thread2InWriteMode = CompletableDeferred<Unit>()
            val finished1 = CompletableDeferred<Unit>()
            val finished2 = CompletableDeferred<Unit>()

            val job1 = launch(dispatcher1) {
                lock.read {
                    thread1InReadMode.complete(Unit)
                    thread2InReadMode.await()
                    lock.write {
                        thread1InWriteMode.complete(Unit)
                    }
                }
                finished1.complete(Unit)
            }

            val job2 = launch(dispatcher2) {
                lock.read {
                    thread2InReadMode.complete(Unit)
                    thread1InReadMode.await()
                    lock.write {
                        thread2InWriteMode.complete(Unit)
                    }
                }
                finished2.complete(Unit)
            }

            joinAll(job1, job2)

            // This is probably not necessary, but let's be 100% sure both write blocks ran
            assertThat(thread1InWriteMode.isCompleted).isTrue()
            assertThat(thread2InWriteMode.isCompleted).isTrue()
        } finally {
            dispatcher1.close()
            dispatcher2.close()
        }
    }
}

