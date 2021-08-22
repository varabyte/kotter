package com.varabyte.konsole.core

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

/**
 * A special variable which can be used to auto-rerender a target [KonsoleBlock] without needing to call
 * [RunUntilScope.rerender] yourself.
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
 */
class KonsoleVar<T>(private var value: T) {
    private var associatedBlockRef: WeakReference<KonsoleBlock>? = null
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        associatedBlockRef = KonsoleBlock.active?.let { WeakReference(it) }
        return value
    }
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        if (this.value != value) {
            this.value = value
            associatedBlockRef?.get()?.let { associatedBlock ->
                KonsoleBlock.active?.let { activeBlock ->
                    if (associatedBlock === activeBlock) activeBlock.requestRerender()
                } ?: run {
                    // Our old block is finished, no need to keep a reference around to it anymore.
                    associatedBlockRef = null
                }
            }
        }
    }
}