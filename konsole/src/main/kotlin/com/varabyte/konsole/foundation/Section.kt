package com.varabyte.konsole.foundation

import com.varabyte.konsole.runtime.Section

fun Section.runUntilSignal(block: suspend Section.RunScope.() -> Unit) {
    run {
        block()
        waitForSignal()
    }
}