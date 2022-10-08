package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.terminal.TestTerminal

/**
 * Like a [session] block but backed by a [TestTerminal], which is provides as a lambda argument.
 *
 * The [TestTerminal] is a useful way to assert that various Kotter commands resulted in expected behavior.
 */
fun testSession(block: Session.(TestTerminal) -> Unit) {
    val terminal = TestTerminal()
    session(terminal = terminal) {
        this.block(terminal)
    }
}