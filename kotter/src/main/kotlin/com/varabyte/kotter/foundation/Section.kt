package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.Section

fun Section.runUntilSignal(block: suspend RunScope.() -> Unit) {
    run {
        block()
        waitForSignal()
    }
}