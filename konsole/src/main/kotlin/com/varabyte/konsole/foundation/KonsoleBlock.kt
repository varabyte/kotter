package com.varabyte.konsole.foundation

import com.varabyte.konsole.runtime.KonsoleBlock

fun KonsoleBlock.runUntilSignal(block: suspend KonsoleBlock.RunScope.() -> Unit) {
    run {
        block()
        waitForSignal()
    }
}