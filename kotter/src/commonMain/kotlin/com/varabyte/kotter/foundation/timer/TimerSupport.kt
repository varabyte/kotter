package com.varabyte.kotter.foundation.timer

import com.varabyte.kotter.platform.concurrent.locks.ReentrantReadWriteLock
import com.varabyte.kotter.platform.concurrent.locks.write
import com.varabyte.kotter.platform.internal.concurrent.annotations.GuardedBy
import com.varabyte.kotter.platform.internal.system.getCurrentTimeMs
import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData
import com.varabyte.kotter.runtime.coroutines.KotterDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal abstract class TimerManager(private val lock: ReentrantReadWriteLock) {
    object Key : ConcurrentScopedData.Key<TimerManager> {
        // We definitely want all timers to stop when the run block exits. This way, we can handle some final logic in
        // `onFinishing` where we know we can modify values without worrying about errant timers clobbering things.
        override val lifecycle = RunScope.Lifecycle
    }

    protected inner class Timer(var duration: Duration, val repeat: Boolean, val key: Any?, val callback: TimerScope.() -> Unit): Comparable<Timer> {
        val enqueuedTime = produceCurrentTime()
        var wakeUpTimeRequested = 0L
        var wakeUpTime = 0L
        init { updateWakeUpTime() }

        fun updateWakeUpTime() {
            require(duration.isPositive()) { "Invalid timer requested with non-positive duration"}

            wakeUpTimeRequested = produceCurrentTime()
            wakeUpTime = wakeUpTimeRequested + duration.inWholeMilliseconds
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
    protected val timers = mutableListOf<Timer>()

    fun addTimer(duration: Duration, repeat: Boolean = false, key: Any? = null, callback: TimerScope.() -> Unit) {
        lock.write {
            if (key == null || timers.none { timer -> key == timer.key }) {
                timers.add(Timer(duration, repeat, key, callback))
            }
        }
    }

    internal fun dispose() {
        lock.write { timers.clear() }
        // Don't wait for this job - we already cleared the timers, so we know it dying is just a formality. If we try
        // to block here waiting for it to end, we'll actually block the main thread itself (which is causing this to
        // get disposed behind the same lock) which would cause a deadlock.

        onDisposed()
    }

    protected fun triggerTimers() {
        lock.write {
            val currTime = produceCurrentTime()
            timers.sort()
            val timersToFire = timers.takeWhile { it.wakeUpTime <= currTime }
            timersToFire.forEach { timer ->
                val scope = TimerScope(
                    timer.duration,
                    timer.repeat,
                    (currTime - timer.wakeUpTimeRequested).milliseconds,
                    (currTime - timer.enqueuedTime).milliseconds,
                )
                timer.callback.invoke(scope)
                // check we actually are adding and removing timers here, because with sorted sets, it's easy to
                // screw up its assumptions (by modifying a value without telling it about the change) and end up
                // with the add / remove operations failing
                check(timers.remove(timer))
                if (scope.repeat) {
                    timer.duration = scope.duration
                    timer.updateWakeUpTime()
                    check(timers.add(timer))
                }
            }
        }
    }

    protected abstract fun produceCurrentTime(): Long

    protected open fun onDisposed() {}
}

internal class SystemTimerManager(lock: ReentrantReadWriteLock) : TimerManager(lock) {
    private val job = CoroutineScope(KotterDispatchers.IO).launch {
        while (isActive) {
            delay(16)
            triggerTimers()
        }
    }

    override fun produceCurrentTime() = getCurrentTimeMs()

    override fun onDisposed() {
        // Don't wait for this job - we already cleared the timers before this point, so we know it dying is just a
        // formality. If we try to block here waiting for it to end, we'll actually block the main thread itself (which
        // is causing this to get disposed behind the same lock) which would cause a deadlock.
        job.cancel()
    }
}

/**
 * See [RunScope.addTimer].
 *
 * This version is the same thing but which works directly on the underlying [ConcurrentScopedData] store, making it
 * a useful helper method for occasionally extension methods which don't have access to the [RunScope] (e.g. animations
 * inside a `section` block).
 */
fun ConcurrentScopedData.addTimer(
    duration: Duration,
    repeat: Boolean,
    key: Any? = null,
    callback: TimerScope.() -> Unit
) {
    putIfAbsent(TimerManager.Key, { SystemTimerManager(lock) }, { timers -> timers.dispose() }) {
        addTimer(duration, repeat, key, callback)
    }
}

/**
 * Values which can be read or modified inside a timer callback (that is, for a timer that was just triggered).
 *
 * @property duration The duration of the timer. This can be modified if you want a repeating timer to have a shorter
 *   followup duration.
 * @property repeat Whether the current timer is repeating or not. It can be modified, so it is most commonly useful to
 *   set this to false if you have a repeating timer that you want to stop at this point.
 * @property elapsed Time actually elapsed since this timer was last fired. This *should* be close to [duration] but it
 *   could possibly lag behind it (since timers don't always fire exactly on time).
 * @property totalElapsed Total time elapsed since the timer was first enqueued. If the timer is repeating, this value
 *   will accumulate over all the runs.
 */
class TimerScope(var duration: Duration, var repeat: Boolean, val elapsed: Duration, val totalElapsed: Duration)

/**
 * Add a timer that will be fired as long as the current section is still running.
 *
 * For example, to create a timer that repeats every half second:
 *
 * ```
 * section {
 *   ...
 * }.runUntilSignal {
 *   addTimer(500.milliseconds, repeat = true) {
 *     ... handle timer fired here ...
 *   }
 *   ...
 * }
 * ```
 *
 * @param duration The amount of time after which this timer's callback will fire. This value can optionally be updated
 *   on later calls by settings [TimerScope.duration] inside your callback.
 * @param repeat If true, repeat this timer indefinitely. You can set [TimerScope.repeat] to false inside your callback
 *   to stop an indefinitely repeating timer.
 * @param key A value to uniquely identify this timer. If set, any followup calls to [addTimer] will be ignored, unless
 *   the previous timer with this key finished running.
 * @param callback Logic to trigger every time the timer runs.
 */
fun RunScope.addTimer(duration: Duration, repeat: Boolean = false, key: Any? = null, callback: TimerScope.() -> Unit) {
    data.addTimer(duration, repeat, key, callback)
}