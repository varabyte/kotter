package com.varabyte.konsole.foundation

import com.varabyte.konsole.runtime.KonsoleApp
import com.varabyte.konsole.runtime.KonsoleBlock
import net.jcip.annotations.ThreadSafe
import java.lang.ref.WeakReference
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write
import kotlin.reflect.KProperty

/**
 * A special variable which can be used to auto-rerender a target [KonsoleBlock] without needing to call
 * [KonsoleBlock.RunScope.rerender] yourself.
 *
 * The way it works is, when this variable is fetched, it is checked if this has happened while we're in an active
 * block:
 *
 * ```
 * var count by KonsoleVar(0)
 * konsole { <-- active block
 *    for (i in 0 until count) { // <-- getValue happens here, gets associated with active block
 *      text("*")
 *    }
 * }.runUntilFinished {
 *   while (count < 5) {
 *     delay(1000)
 *     ++count // <-- setValue happens here, causes active block to rerun
 *   }
 * }
 *
 * count = 123 // Setting count out of a konsole block is fine; nothing is triggered
 * ```
 *
 * This class's value can be queried and set across different values, so it is designed to be thread safe.
 */
@ThreadSafe
class KonsoleVar<T> internal constructor(private val app: KonsoleApp, private var value: T) {

    private var associatedBlockRef: WeakReference<KonsoleBlock>? = null
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        return app.data.lock.read {
            associatedBlockRef = app.activeBlock?.let { WeakReference(it) }
            value
        }
    }
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        app.data.lock.write {
            if (this.value != value) {
                this.value = value
                associatedBlockRef?.get()?.let { associatedBlock ->
                    app.activeBlock?.let { activeBlock ->
                        if (associatedBlock === activeBlock) activeBlock.requestRerender()
                    } ?: run {
                        // Our old block is finished, no need to keep a reference around to it anymore.
                        associatedBlockRef = null
                    }
                }
            }
        }
    }
}

/** Create a [KonsoleVar] whose scope is tied to this app. */
@Suppress("FunctionName") // Intentionally made to look like a class constructor
fun <T> KonsoleApp.konsoleVarOf(value: T): KonsoleVar<T> = KonsoleVar(this, value)