package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.terminal.TestTerminal

fun testSession(block: Session.(TestTerminal) -> Unit) {
    val terminal = TestTerminal()
    session(terminal = terminal) {
        this.block(terminal)
    }
}