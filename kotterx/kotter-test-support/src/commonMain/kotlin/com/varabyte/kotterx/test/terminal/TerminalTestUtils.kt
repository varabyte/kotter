package com.varabyte.kotterx.test.terminal

import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotter.runtime.terminal.mock.*
import com.varabyte.kotterx.test.runtime.*
import com.varabyte.kotterx.util.*

/**
 * A way to assert that the current terminal's output matches that of another render block.
 *
 * This can be useful if the current terminal went through a bunch of complex rerenders and events, where you want to
 * now assert that is has settled onto some final state. For example:
 *
 * ```
 * testSession { terminal ->
 *   section { ... do some crazy stuff here, much which gets erased ... }.run { ... more crazy stuff ... }
 *
 *   terminal.assertMatches {
 *     green { textLine("Expected final text!") }
 *   }
 * }
 * ```
 *
 * This method will throw an [AssertionError] containing more information if the two renders don't match.
 */
fun MockTerminal.assertMatches(expected: RenderScope.() -> Unit) {
    val oursResolved = this.resolveRerenders()
    val theirsResolved = buildAnsiLines(expected)

    if (oursResolved != theirsResolved) {
        throw AssertionError(buildString {
            appendLine("Text render output does not match.")
            appendLine()
            appendLine("Ours:")
            oursResolved.forEach { line -> appendLine("\t${line.highlightControlCharacters()}") }
            appendLine()
            appendLine("Expected:")
            theirsResolved.forEach { line -> appendLine("\t${line.highlightControlCharacters()}") }
        })
    }
}

/**
 * Similar to [assertMatches] but just returns a boolean value instead of throwing an assertion.
 */
fun MockTerminal.matches(expected: RenderScope.() -> Unit): Boolean {
    return this.resolveRerenders() == buildAnsiLines(expected)
}
