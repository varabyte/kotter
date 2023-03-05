package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.truthish.assertThat
import kotlinx.coroutines.*
import kotlin.test.Test

// NOTE: If this test seems sparse for such potentially delicate code, it's because the general usage of the lock class
// is already tested by the rest of the tests indirectly. However, as for now (this may change later), this class exists
// to test really gnarly edge cases I want to make sure are working
class ReentrantReadWriteLockTest {

    @Test
    fun twoThreadsCanConvertReadToWriteLocksAtTheSameTime() {
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

            CoroutineScope(dispatcher1).launch {
                lock.read {
                    thread1InReadMode.complete(Unit)
                    thread2InReadMode.await()
                    lock.write {
                        thread1InWriteMode.complete(Unit)
                    }
                }
                finished1.complete(Unit)
            }

            CoroutineScope(dispatcher2).launch {
                lock.read {
                    thread2InReadMode.complete(Unit)
                    thread1InReadMode.await()
                    lock.write {
                        thread2InWriteMode.complete(Unit)
                    }
                }
                finished2.complete(Unit)
            }

            runBlocking {
                listOf(finished1, finished1).awaitAll()
            }

            // This is probably not necessary, but let's be 100% sure both write blocks ran
            assertThat(thread1InWriteMode.isCompleted).isTrue()
            assertThat(thread2InWriteMode.isCompleted).isTrue()
        } finally {
            dispatcher1.close()
            dispatcher2.close()
        }
    }

}