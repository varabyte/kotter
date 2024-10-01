package com.varabyte.kotterx.util

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotter.runtime.terminal.mock.*

/**
 * A helper method to generate ANSI output from a lambda block.
 *
 * For example:
 *
 * ```kotlin
 * println(consoleOutputFor {
 *   red { text("Hello, ") }
 *   green { text("World!") }
 * })
 * ```
 */
fun consoleOutputFor(block: RenderScope.() -> Unit): String = consoleLinesFor(block).joinToString("\n")

fun consoleLinesFor(block: RenderScope.() -> Unit): List<String> {
    val terminal = MockTerminal()
    session(terminal) {
        section {
            block()
        }.run()
    }
    return terminal.resolveRerenders()
}