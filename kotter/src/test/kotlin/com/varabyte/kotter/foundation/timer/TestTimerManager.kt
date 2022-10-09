package com.varabyte.kotter.foundation.timer

import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData
import java.time.Duration
import java.util.concurrent.locks.ReentrantReadWriteLock

// Note: Class needs to be internal because TimerManager is internal
internal class TestTimerManager(lock: ReentrantReadWriteLock) : TimerManager(lock) {
    private var currentTime: Long = 0
    fun fastForward(duration: Duration) {
        if (duration.isNegative || duration.isZero) return

        currentTime += duration.toMillis()
        triggerTimers()
    }

    override fun produceCurrentTime() = currentTime
}

/** Provides a simpler API to the internal [TestTimerManager] class. */
class TestTimer internal constructor(private val timerManager: TestTimerManager) {
    fun fastForward(duration: Duration) {
        timerManager.fastForward(duration)
    }
}

/**
 * Create a fake timer that can control the time used by Kotter's [addTimer] functionality.
 *
 * This function MUST be declared in a run block and will fail if not set up before
 */
fun ConcurrentScopedData.useTestTimer(): TestTimer {
    require(isActive(RunScope.Lifecycle)) {
        "This method can only be called inside a `run` block."
    }

    val testTimerManager = TestTimerManager(lock)
    if (!tryPut(TimerManager.Key) { testTimerManager }) {
        error("Attempted to initialize this test with a test timer after a different timer was already created.")
    }

    return TestTimer(testTimerManager)
}
