package com.varabyte.konsole.core

import kotlin.reflect.KProperty

class KonsoleVar<T>(private var value: T) {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        if (this.value != value) {
            this.value = value
            KonsoleBlock.active?.requestRerender()
        }
    }
}