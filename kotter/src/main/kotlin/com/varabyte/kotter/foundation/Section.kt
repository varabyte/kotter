package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.Section

fun Section.runUntilSignal(block: suspend Section.RunScope.() -> Unit) {
    run {
        block()
        waitForSignal()
    }
}