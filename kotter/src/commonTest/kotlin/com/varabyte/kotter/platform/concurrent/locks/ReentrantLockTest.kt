package com.varabyte.kotter.platform.concurrent.locks

import com.varabyte.kotter.platform.internal.concurrent.Thread
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class ReentrantLockTest {
    @Test
    fun `basic lock behavior works`() {
        val lock = ReentrantLock()
        var counter = 0

        lock.withLock {
            counter++
        }

        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun `lock is reentrant on the same thread`() {
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

    @Test
    fun `unlock without lock throws exception`() {
        val lock = ReentrantLock()

        // Calling unlock on an idle lock should throw an IllegalMonitorStateException
        assertThrows<IllegalStateException> {
            lock.unlock()
        }
    }

    @Test
    fun `unlocking from different thread throws exception`() = runTest {
        // Use real threads and not fake coroutine threads because lock / unlock uses real thread IDs under the hood to
        // manage who owns any active read locks or write lock.
        val ownerDispatcher = newFixedThreadPoolContext(1, "Owner Thread")
        val thiefDispatcher = newFixedThreadPoolContext(1, "Thief Thread")

        try {
            val lock = ReentrantLock()
            val ownerAcquiredLock = CompletableDeferred<Unit>()
            val thiefFinished = CompletableDeferred<Unit>()

            val ownerJob = CoroutineScope(ownerDispatcher).launch {
                lock.lock() // Lock acquired on Thread 1
                ownerAcquiredLock.complete(Unit)
                thiefFinished.await()
                lock.unlock()
            }

            val thiefJob = CoroutineScope(thiefDispatcher).launch {
                ownerAcquiredLock.await()

                // Thread 2 attempts to unlock Thread 1's lock
                assertThrows<IllegalStateException> {
                    lock.unlock()
                }

                thiefFinished.complete(Unit)
            }

            thiefFinished.await()
            joinAll(ownerJob, thiefJob)
        } finally {
            ownerDispatcher.close()
            thiefDispatcher.close()
        }
    }

    @Test
    fun `lock blocks other threads until released`() = runTest {
        // Use real threads and not fake coroutine threads because lock / unlock uses real thread IDs under the hood to
        // manage who owns any active read locks or write lock.
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