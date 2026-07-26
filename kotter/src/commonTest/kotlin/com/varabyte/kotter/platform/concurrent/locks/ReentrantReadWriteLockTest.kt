package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.internal.concurrent.Thread
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class ReentrantReadWriteLockTest {
    @Test
    fun `read lock can be upgraded to write lock`() {
        val lock = ReentrantReadWriteLock()

        lock.reader.lock()
        lock.writer.lock()
    }

    @Test
    fun `write lock can always allow read lock`() {
        val lock = ReentrantReadWriteLock()

        lock.writer.lock()
        lock.reader.lock()
    }

    @Test
    fun `attempting to unlock locks you do not have throws exception`() {
        val lock = ReentrantReadWriteLock()

        lock.reader.lock()
        lock.writer.lock()
        lock.reader.lock()
        lock.reader.unlock()
        lock.writer.unlock()

        assertThrows<IllegalStateException> {
            lock.writer.unlock()
        }

        lock.reader.unlock()

        assertThrows<IllegalStateException> {
            lock.writer.unlock()
        }

        assertThrows<IllegalStateException> {
            lock.reader.unlock()
        }
    }


    @Test
    fun `can release locks in any order`() {
        // In practice, people should be doing strictly nested locking,
        // e.g. "+R, +W, +R, -R, -W, -R"
        // but the algorithm can handle any order as long as the lock / unlocks are matched

        fun <T> getUniquePermutations(list: List<T>): Sequence<List<T>> {
            if (list.isEmpty()) return sequenceOf(emptyList())

            return sequence {
                val seen = mutableSetOf<T>()
                for (i in list.indices) {
                    val element = list[i]

                    if (seen.add(element)) {
                        val remaining = list.take(i) + list.drop(i + 1)
                        for (permutation in getUniquePermutations(remaining)) {
                            yield(listOf(element) + permutation)
                        }
                    }
                }
            }
        }

        val baseOps = listOf('R', 'R', 'W', 'W')

        val lockSequences = getUniquePermutations(baseOps)
        val unlockSequences = getUniquePermutations(baseOps)

        for (lockSeq in lockSequences) {
            for (unlockSeq in unlockSequences) {
                val lock = ReentrantReadWriteLock()

                for (op in lockSeq) {
                    when (op) {
                        'R' -> lock.reader.lock()
                        'W' -> lock.writer.lock()
                    }
                }

                for (op in unlockSeq) {
                    when (op) {
                        'R' -> lock.reader.unlock()
                        'W' -> lock.writer.unlock()
                    }
                }
            }
        }
    }

    @Test
    fun `multiple concurrent readers are allowed`() = runTest {
        // Use real threads and not fake coroutine threads because lock / unlock uses real thread IDs under the hood to
        // manage who owns any active read locks or write lock.
        val dispatchers = listOf(
            newFixedThreadPoolContext(1, "Thread 1"),
            newFixedThreadPoolContext(1, "Thread 2"),
            newFixedThreadPoolContext(1, "Thread 3")
        )

        try {
            val lock = ReentrantReadWriteLock()

            val readerEntered = Channel<Unit>(capacity = 3)
            val releaseReaders = CompletableDeferred<Unit>()
            val finishedCount = Channel<Unit>(capacity = 3)

            val jobs = List(3) { i ->
                launch(dispatchers[i]) {
                    lock.read {
                        readerEntered.send(Unit)
                        releaseReaders.await()
                    }
                    finishedCount.send(Unit)
                }
            }

            repeat(3) { readerEntered.receive() }

            // If read locks weren't concurrent, we would deadlock here because the first reader would never exit to let
            // the next one enter. Since we reached this line, concurrency is proven. Release them!
            releaseReaders.complete(Unit)

            repeat(3) { finishedCount.receive() }
            jobs.joinAll()
        } finally {
            dispatchers.forEach { it.close() }
        }
    }

    enum class WriteLockState {
        NOT_STARTED,
        ENTERED,
        FINISHING,
        FINISHED,
    }

    @Test
    fun `writer excludes readers`() = runTest {
        // Use real threads and not fake coroutine threads because lock / unlock uses real thread IDs under the hood to
        // manage who owns any active read locks or write lock.
        val writeDispatcher = newFixedThreadPoolContext(1, "Writing thread")
        val readDispatcher = newFixedThreadPoolContext(1, "Reading thread")

        try {

            val lock = ReentrantReadWriteLock()

            val writerInside = CompletableDeferred<Unit>()
            val readerFinished = CompletableDeferred<Unit>()
            val writerFinished = CompletableDeferred<Unit>()

            var writeLockState = WriteLockState.NOT_STARTED
            var readerObservedState: WriteLockState? = null

            // Thread 1: Claims the write lock
            launch(writeDispatcher) {
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
            launch(readDispatcher) {
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
        } finally {
            writeDispatcher.close()
            readDispatcher.close()
        }
    }

    @Test
    fun `read locks have consistent thread affinity`() {
        val currThreadId = Thread.getId()
        val lock = ReentrantReadWriteLock()

        lock.read {
            assertThat(Thread.getId()).isEqualTo(currThreadId)
            lock.read {
                assertThat(Thread.getId()).isEqualTo(currThreadId)
                lock.read {
                    assertThat(Thread.getId()).isEqualTo(currThreadId)
                }
            }
        }
    }

    @Test
    fun `write locks have consistent thread affinity`() {
        val currThreadId = Thread.getId()
        val lock = ReentrantReadWriteLock()

        lock.write {
            assertThat(Thread.getId()).isEqualTo(currThreadId)
            lock.write {
                assertThat(Thread.getId()).isEqualTo(currThreadId)
                lock.write {
                    assertThat(Thread.getId()).isEqualTo(currThreadId)
                }
            }
        }
    }

    @Test
    fun `mixed locks have consistent thread affinity`() {
        val currThreadId = Thread.getId()
        val lock = ReentrantReadWriteLock()

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
                    }
                }
            }
        }
    }

    @Test
    fun `two threads can convert read to write locks without deadlocking`() = runTest {
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
            val finished1 = CompletableDeferred<Unit>()
            val finished2 = CompletableDeferred<Unit>()

            val job1 = launch(dispatcher1) {
                lock.read {
                    thread1InReadMode.complete(Unit)
                    thread2InReadMode.await()
                    lock.write {}
                }
                finished1.complete(Unit)
            }

            val job2 = launch(dispatcher2) {
                lock.read {
                    thread2InReadMode.complete(Unit)
                    thread1InReadMode.await()
                    lock.write {}
                }
                finished2.complete(Unit)
            }

            joinAll(job1, job2)
        } finally {
            dispatcher1.close()
            dispatcher2.close()
        }
    }

    @Test
    fun `nested read lock request does not deadlock with nested write request`() = runTest {
        // There was an earlier version of our code where we were trying to be nice and have any read requests detect
        // if a write request was trying to happen, and if so, let it go first. However...
        //
        // If one threads is here, inside an outer read trying to request an inner read:
        //
        // ```
        // lock.read {
        //    lock.read { ... } <----
        // }
        // ```
        //
        // And the second thread is here, inside an outer read trying to request an inner write:
        //
        // ```
        // lock.read {
        //    lock.write { ... } <----
        // }
        // ```
        //
        // we would deadlock! Because the write lock would pause its current read lock, and then the other thread's
        // read lock would detect that newly paused read thread, and then block, waiting for the other thread's write
        // lock to happen.

        val dispatcher1 = newFixedThreadPoolContext(1, "Thread 1")
        val dispatcher2 = newFixedThreadPoolContext(1, "Thread 2")

        try {
            val lock = ReentrantReadWriteLock()
            val thread1InReadMode = CompletableDeferred<Unit>()
            val thread2InReadMode = CompletableDeferred<Unit>()
            val finished1 = CompletableDeferred<Unit>()
            val finished2 = CompletableDeferred<Unit>()

            val job1 = launch(dispatcher1) {
                lock.read {
                    thread1InReadMode.complete(Unit)
                    thread2InReadMode.await()
                    lock.read {}
                }
                finished1.complete(Unit)
            }

            val job2 = launch(dispatcher2) {
                lock.read {
                    thread2InReadMode.complete(Unit)
                    thread1InReadMode.await()
                    lock.write {}
                }
                finished2.complete(Unit)
            }

            joinAll(job1, job2)
        } finally {
            dispatcher1.close()
            dispatcher2.close()
        }
    }
}

