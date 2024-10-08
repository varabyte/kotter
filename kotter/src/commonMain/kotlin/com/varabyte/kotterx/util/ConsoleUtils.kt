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
 * println(buildAnsiString {
 *   red { text("Hello, ") }
 *   green { text("World!") }
 * })
 * ```
 */
fun buildAnsiString(block: RenderScope.() -> Unit): String = buildAnsiLines(block).joinToString("\n")

/**
 * Like [buildAnsiString] but where each line is broken up into a list of strings.
 */
fun buildAnsiLines(block: RenderScope.() -> Unit): List<String> {
    val terminal = MockTerminal()
    session(terminal) {
        section {
            block()
        }.run()
    }
    return terminal.resolveRerenders()
}