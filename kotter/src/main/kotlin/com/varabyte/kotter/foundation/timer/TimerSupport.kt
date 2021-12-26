package com.varabyte.kotter.foundation.timer

import com.varabyte.kotter.runtime.Section
import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData
import kotlinx.coroutines.*
import net.jcip.annotations.GuardedBy
import java.time.Duration
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

internal class TimerManager(private val lock: ReentrantReadWriteLock) {
    object Key : ConcurrentScopedData.Key<TimerManager> {
        override val lifecycle = Section.RunScope.Lifecycle
    }

    private class Timer(var duration: Duration, val repeat: Boolean, val key: Any?, val callback: TimerScope.() -> Unit): Comparable<Timer> {
        val enqueuedTime = System.currentTimeMillis()
        var wakeUpTimeRequested = 0L
        var wakeUpTime = 0L
        init { updateWakeUpTime() }

        fun updateWakeUpTime() {
            require(!duration.isZero && !duration.isNegative) { "Invalid timer requested with non-positive duration"}

            wakeUpTimeRequested = System.currentTimeMillis()
            wakeUpTime = wakeUpTimeRequested + duration.toMillis()
        }
        override fun compareTo(other: Timer): Int {
            // By default, we want to sort by wakeup time, but if two timers have the exact wakeup time, then the order
            // doesn't matter, but we have to return SOMETHING non-zero, or else some algorithm will think the two
            // timers are the same. We use hashCode because it's convenient, it's consistent, and it doesn't really
            // matter.
            return (wakeUpTime.compareTo(other.wakeUpTime)).takeIf { it != 0 } ?: return hashCode().compareTo(other.hashCode())
        }
    }

    @GuardedBy("lock")
    private val timers = sortedSetOf<Timer>()

    private val job = CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            delay(16)
            lock.write {
                val currTime = System.currentTimeMillis()
                val timersToFire = timers.takeWhile { it.wakeUpTime <= currTime }
                timersToFire.forEach { timer ->
                    val scope = TimerScope(
                        timer.duration,
                        timer.repeat,
                        Duration.ofMillis(currTime - timer.wakeUpTimeRequested),
                        Duration.ofMillis(currTime - timer.enqueuedTime)
                    )
                    timer.callback.invoke(scope)
                    if (scope.repeat) {
                        timer.duration = scope.duration
                        timer.updateWakeUpTime()
                    }
                    else {
                        timers.remove(timer)
                    }
                }
            }
        }
    }

    fun addTimer(duration: Duration, repeat: Boolean = false, key: Any? = null, callback: TimerScope.() -> Unit) {
        lock.write {
            if (key == null || timers.none { timer -> key == timer.key }) {
                timers.add(Timer(duration, repeat, key, callback))
            }
        }
    }

    fun dispose() {
        lock.write { timers.clear() }
        // Don't wait for this job - we already cleared the timers, so we know it dying is just a formality. If we try
        // to block here waiting for it to end, we'll actually block the main thread itself (which is causing this to
        // get disposed behind the same lock) which would cause a deadlock.
        job.cancel()
    }
}

/**
 * See [Section.RunScope.addTimer].
 *
 * This version is the same thing but which works directly on the underlying [ConcurrentScopedData] store, making it
 * a useful helper method for other internal methods to use.
 */
fun ConcurrentScopedData.addTimer(duration: Duration, repeat: Boolean, key: Any? = null, callback: TimerScope.() -> Unit) {
    putIfAbsent(TimerManager.Key, { TimerManager(lock) }, { timers -> timers.dispose() }) {
        addTimer(duration, repeat, key, callback)
    }
}

/**
 * Values which can be read or modified inside a timer callback.
 *
 * @param elapsed Time actually elapsed since this timer was last fired. This *should* be close to [duration] but it
 *   could possible lag behind it.
 * @param totalElapsed Total time elapsed since the timer was first enqueued. If the timer is repeating, this value
 *   will accumulate over all the runs.
 */
class TimerScope(var duration: Duration, var repeat: Boolean, val elapsed: Duration, val totalElapsed: Duration)

/**
 * Add a timer that will be fired as long as the current section is still running.
 *
 * @param duration The amount of time after which this timer's callback will fire. This value can optionally be updated
 *   on later calls by settings [TimerScope.duration] inside your callback.
 * @param repeat If true, repeat this timer indefinitely. You can set [TimerScope.repeat] to false inside your callback
 *   to stop an indefinitely repeating timer.
 * @param key A value to uniquely identify this timer. If set, any followup calls to [addTimer] will be ignored, unless
 *   the previous timer with this key finished running.
 * @param callback Logic to trigger every time the timer runs.
 */
fun Section.RunScope.addTimer(duration: Duration, repeat: Boolean = false, key: Any? = null, callback: TimerScope.() -> Unit, ) {
    data.addTimer(duration, repeat, key, callback)
}