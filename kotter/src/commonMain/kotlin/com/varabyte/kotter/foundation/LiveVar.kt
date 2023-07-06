package com.varabyte.kotter.foundation

import com.varabyte.kotter.platform.concurrent.locks.*
import com.varabyte.kotter.platform.internal.concurrent.annotations.*
import com.varabyte.kotter.platform.internal.ref.*
import com.varabyte.kotter.runtime.*
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
class LiveVar<T> internal constructor(private val session: Session, value: T) {
    private var associatedSectionRef: WeakReference<Section>? = null
    var value: T = value
        get() = session.data.lock.read {
            associatedSectionRef = session.activeSection?.let { WeakReference(it) }
            field
        }
        set(value) {
            session.data.lock.write {
                if (field != value) {
                    field = value
                    associatedSectionRef?.get()?.let { associatedBlock ->
                        session.activeSection?.let { activeSection ->
                            if (associatedBlock === activeSection) activeSection.requestRerender()
                        } ?: run {
                            // Our old block is finished, no need to keep a reference around to it anymore.
                            associatedSectionRef = null
                        }
                    }
                }
            }
        }

    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        this.value = value
    }
}

/** Create a [LiveVar] whose scope is tied to this session. */
fun <T> Session.liveVarOf(value: T): LiveVar<T> = LiveVar(this, value)
