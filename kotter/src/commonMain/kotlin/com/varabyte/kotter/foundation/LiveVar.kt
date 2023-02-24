package com.varabyte.kotter.foundation

import com.varabyte.kotter.platform.concurrent.annotations.ThreadSafe
import com.varabyte.kotter.platform.ref.WeakReference
import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.Section
import kotlin.reflect.KProperty

/**
 * A special variable which can be used to auto-rerender a target [Section] without needing to call
 * [RunScope.rerender] yourself.
 *
 * For example:
 *
 * ```
 * var count by liveVarOf(0)
 * section {
 *    for (i in 0 until count) { // <-- LiveVar read happens here
 *      text("*")
 *    }
 * }.runUntilFinished {
 *   while (count < 5) {
 *     delay(1000)
 *     ++count // <-- LiveVar write happens here; rerender is triggered
 *   }
 * }
 *
 * count = 123 // Setting LiveVar out of a section is harmless; no rerender is triggered
 * ```
 *
 * This class is thread safe and expected to be accessed across different threads.
 */
@ThreadSafe
class LiveVar<T> internal constructor(private val session: Session, private var value: T) {

    private var associatedBlockRef: WeakReference<Section>? = null
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        return session.data.lock.read {
            associatedBlockRef = session.activeSection?.let { WeakReference(it) }
            value
        }
    }
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        session.data.lock.write {
            if (this.value != value) {
                this.value = value
                associatedBlockRef?.get()?.let { associatedBlock ->
                    session.activeSection?.let { activeSection ->
                        if (associatedBlock === activeSection) activeSection.requestRerender()
                    } ?: run {
                        // Our old block is finished, no need to keep a reference around to it anymore.
                        associatedBlockRef = null
                    }
                }
            }
        }
    }
}

/** Create a [LiveVar] whose scope is tied to this session. */
fun <T> Session.liveVarOf(value: T): LiveVar<T> = LiveVar(this, value)