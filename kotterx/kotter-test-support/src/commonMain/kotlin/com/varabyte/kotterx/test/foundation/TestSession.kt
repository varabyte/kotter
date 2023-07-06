package com.varabyte.kotterx.test.foundation

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.runtime.*
import com.varabyte.kotterx.test.terminal.*

/**
 * Like a [session] block but backed by a [TestTerminal], which is provides as a lambda argument.
 *
 * The [TestTerminal] is a useful way to assert that various Kotter commands resulted in expected behavior.
 */
fun testSession(
    provideWidth: (() -> Int)? = null,
    provideHeight: (() -> Int)? = null,
    block: Session.(TestTerminal) -> Unit
) {
    val terminal = TestTerminal(provideWidth, provideHeight)
    session(terminal = terminal) {
        this.block(terminal)
    }
}
