package com.varabyte.konsole.foundation.timer

import com.varabyte.konsole.runtime.KonsoleBlock
import com.varabyte.konsole.runtime.concurrent.ConcurrentScopedData
import kotlinx.coroutines.*
import java.time.Duration

internal class TimerManager {
    object Key : ConcurrentScopedData.Key<TimerManager> {
        override val lifecycle = KonsoleBlock.RunScope.Lifecycle
    }

    private class Timer(var duration: Duration, val repeat: Boolean, val callback: TimerScope.() -> Unit): Comparable<Timer> {
        val enqueuedTime = System.currentTimeMillis()
        var wakeUpTime = 0L
        init { updateWakeUpTime() }

        fun updateWakeUpTime() {
            wakeUpTime = System.currentTimeMillis() + duration.toMillis()
        }
        override fun compareTo(other: Timer): Int {
            // By default, we want to sort by wakeup time, but if two timers have the exact wakeup time, then the order
            // doesn't matter, but we have to return SOMETHING non-zero, or else some algorithm will think the two
            // timers are the same. We use hashCode because it's convenient, it's consistent, and it doesn't really
            // matter.
            return (wakeUpTime.compareTo(other.wakeUpTime)).takeIf { it != 0 } ?: return hashCode().compareTo(other.hashCode())
        }
    }
    private val timers = sortedSetOf<Timer>()

    private val job = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            delay(16)
            val currTime = System.currentTimeMillis()
            val timersToFire = timers.takeWhile { it.wakeUpTime <= currTime }
            timersToFire.forEach { timer ->
                timers.remove(timer)
                val scope = TimerScope(timer.duration, timer.repeat, Duration.ofMillis(currTime - timer.enqueuedTime))
                timer.callback.invoke(scope)
                if (scope.repeat) {
                    timer.duration = scope.duration
                    timer.updateWakeUpTime()
                    timers.add(timer)
                }
            }
        }
    }

    fun addTimer(duration: Duration, repeat: Boolean = false, callback: TimerScope.() -> Unit) {
        timers.add(Timer(duration, repeat, callback))
    }

    fun dispose() {
        // Intentionally block on dispose here, to ensure we wait for any in flight timers to fire before continuing
        runBlocking { job.cancelAndJoin() }
    }
}

internal fun ConcurrentScopedData.addTimer(duration: Duration, repeat: Boolean, callback: TimerScope.() -> Unit) {
    putIfAbsent(TimerManager.Key, { TimerManager() }, { timers -> timers.dispose() }) {
        addTimer(duration, repeat, callback)
    }
}

/**
 * Values which can be read or modified inside a timer callback.
 *
 * @param totalElapsed Total time elapsed since the timer was first enqueued. If the timer is repeating, this value
 *   will accumulate over all the runs.
 */
class TimerScope(var duration: Duration, var repeat: Boolean, val totalElapsed: Duration)

/**
 * Add a timer that will be fired as long as the current Konsole block is still running.
 *
 * @param duration The amount of time after which this timer's callback will fire. This value can optionally be updated
 *   on later calls by settings [TimerScope.duration] inside your callback.
 * @param repeat If true, repeat this timer indefinitely. You can set [TimerScope.repeat] to false inside your callback
 *   to stop an indefinitely repeating timer.
 * @param callback Logic to trigger every time the timer runs.
 */
fun KonsoleBlock.RunScope.addTimer(duration: Duration, repeat: Boolean = false, callback: TimerScope.() -> Unit) {
    data.addTimer(duration, repeat, callback)
}