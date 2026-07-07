package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.internal.concurrent.Thread
import com.varabyte.truthish.assertThat
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class ReentrantLockTest {
    @Test
    fun basicLockAndUnlockExecutesSequentially() {
        val lock = ReentrantLock()
        var counter = 0

        lock.withLock {
            counter++
        }

        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun lockIsReentrantOnTheSameThread() {
        val currThreadId = Thread.getId()
        val lock = ReentrantLock()
        var counter = 0

        // The same thread should be able to acquire the lock multiple times recursively
        lock.withLock {
            assertThat(Thread.getId()).isEqualTo(currThreadId)
            lock.withLock {
                assertThat(Thread.getId()).isEqualTo(currThreadId)
                lock.withLock {
                    assertThat(Thread.getId()).isEqualTo(currThreadId)
                    counter++
                }
            }
        }

        assertThat(counter).isEqualTo(1)
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @Test
    fun lockBlocksOtherThreadsUntilReleased() = runTest {
        val dispatcher1 = newFixedThreadPoolContext(1, "Thread 1")
        val dispatcher2 = newFixedThreadPoolContext(1, "Thread 2")

        try {
            val lock = ReentrantLock()
            val thread1Acquired = CompletableDeferred<Unit>()
            val thread2TryLock = CompletableDeferred<Unit>()
            val thread1CanRelease = CompletableDeferred<Unit>()
            val thread2Finished = CompletableDeferred<Unit>()

            var sharedResource = 0

            CoroutineScope(dispatcher1).launch {
                lock.withLock {
                    sharedResource = 1
                    thread1Acquired.complete(Unit)

                    // Force Thread 1 to hold the lock while Thread 2 tries to grab it
                    thread2TryLock.await()
                    sharedResource = 2

                    thread1CanRelease.complete(Unit)
                }
            }

            CoroutineScope(dispatcher2).launch {
                thread1Acquired.await()

                // Signal Thread 1 that we are about to try acquiring the lock
                thread2TryLock.complete(Unit)

                lock.withLock {
                    // This block MUST NOT run until Thread 1 completely exits its withLock
                    assertThat(sharedResource).isEqualTo(2)
                    sharedResource = 3
                }
                thread2Finished.complete(Unit)
            }

            thread1CanRelease.await()
            thread2Finished.await()

            assertThat(sharedResource).isEqualTo(3)
        } finally {
            dispatcher1.close()
            dispatcher2.close()
        }
    }
}