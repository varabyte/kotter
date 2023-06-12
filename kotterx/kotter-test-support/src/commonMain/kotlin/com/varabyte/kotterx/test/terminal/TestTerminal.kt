package com.varabyte.kotterx.test.terminal

import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.startsWith
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotter.runtime.terminal.Terminal
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.kotterx.test.runtime.replaceControlCharacters
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.math.min

/**
 * A fake terminal, built for tests, which stores data written to it in memory that can then be queried.
 */
class TestTerminal(private val provideWidth: (() -> Int)? = null, private val provideHeight: (() -> Int)? = null)
    : Terminal {
    companion object {
        /**
         * Helper function that generates the final console output for a given, simple Kotter block.
         *
         * This is useful for verifying the result of some complex Kotter block like so:
         *
         * ```
         * testSession { terminal ->
         *   section {
         *      /* do some crazy stuff with lots of intermediate rewrites that end up just printing "Hello world" */
         *   }.run()
         *
         *   assertThat(terminal.resolveRerenders()).isEqualTo(TestTerminal.consoleOutputFor {
         *     textLine("Hello world")
         *   })
         * }
         */
        fun consoleOutputFor(block: RenderScope.() -> Unit): List<String> {
            lateinit var output: List<String>
            testSession { terminal ->
                section {
                    block()
                }.run()

                output = terminal.resolveRerenders()
            }

            return output
        }
    }

    var closed = false
        private set

    private val _buffer = StringBuilder()
    val buffer get() = _buffer.toString()

    private val keysChannel = Channel<Int>()

    suspend fun sendKeys(vararg keys: Int) {
        keys.forEach { keysChannel.send(it) }
    }

    // TODO: Allow tests to set this so we can verify width wrapping behavior.
    override val width get() = provideWidth?.invoke() ?: Int.MAX_VALUE
    override val height get() = provideHeight?.invoke() ?: Int.MAX_VALUE // In memory text doesn't have a height limit

    override fun write(text: String) {
        assertNotClosed()
        _buffer.append(text)
    }

    override fun read(): Flow<Int> {
        assertNotClosed()
        return keysChannel.consumeAsFlow()
    }

    override fun clear() {
        assertNotClosed()
        _buffer.clear()
    }

    override fun close() {
        assertNotClosed()
        closed = true
    }

    private fun assertNotClosed() {
        check(!closed) { "Tried to modify this terminal after it was closed" }
    }
}

/** Convenience function for the common case of only sending a single key. */
suspend fun TestTerminal.sendKey(key: Int) = sendKeys(key)

/** Convenience function for the common case of sending an ANSI code. */
suspend fun TestTerminal.sendCode(code: Ansi.Csi.Code) = type(*code.toFullEscapeCode().toCharArray())

/** Convenience functions for typing characters (instead of sending their underlying codes) */
suspend fun TestTerminal.type(vararg chars: Char) = sendKeys(*chars.map { it.code }.toIntArray())
suspend fun TestTerminal.type(text: String) = type(*text.toCharArray())

/**
 * Convenience method that returns this test terminal's [TestTerminal.buffer] as separate lines.
 */
fun TestTerminal.lines(): List<String> {
    return buffer.split("\n")
}

/**
 * Return the state of the terminal AFTER lines have been erased and repainted.
 */
fun TestTerminal.resolveRerenders(): List<String> {
    val codeEraseToLineEnd = Codes.Erase.CURSOR_TO_LINE_END.toFullEscapeCode()
    val codeMoveToPrevLine = Codes.Cursor.MOVE_TO_PREV_LINE.toFullEscapeCode()

    val resolved = mutableListOf<String>()
    val currLine = StringBuilder()
    var currLineIndex = 0
    val textPtr = TextPtr(buffer)
    while (textPtr.remainingLength > 0) {
        // Store the current char index so we can check it later. Some handlers might change it, and if so,
        // we should not attempt to modify it further.
        val textPtrCharIndex = textPtr.charIndex
        when {
            textPtr.currChar == '\r' -> {
                currLineIndex = 0
            }
            textPtr.currChar == '\n' -> {
                resolved.add(currLine.toString())
                currLine.clear()
                currLineIndex = 0
            }
            textPtr.startsWith(codeEraseToLineEnd) -> {
                textPtr.charIndex += codeEraseToLineEnd.length
                currLine.deleteRange(currLineIndex, currLineIndex + currLine.length)
            }
            textPtr.startsWith(codeMoveToPrevLine) -> {
                textPtr.charIndex += codeMoveToPrevLine.length
                resolved.removeLast()
                currLine.clear()
                resolved.firstOrNull()?.let { currLine.append(it) }
                currLineIndex = min(currLineIndex, currLine.lastIndex)
            }
            else -> {
                currLine.insert(currLineIndex++, textPtr.currChar)
            }
        }

        if (textPtrCharIndex == textPtr.charIndex) {
            textPtr.increment()
        }
    }

    resolved.add(currLine.toString())
    return resolved
}

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
fun TestTerminal.assertMatches(expected: RenderScope.() -> Unit) {
    val oursResolved = this.resolveRerenders()
    val theirsResolved = TestTerminal.consoleOutputFor(expected)

    if (oursResolved != theirsResolved) {
        throw AssertionError(buildString {
            appendLine("Text render output does not match.")
            appendLine()
            appendLine("Ours:")
            oursResolved.forEach { line -> appendLine("\t${line.replaceControlCharacters()}") }
            appendLine()
            appendLine("Expected:")
            theirsResolved.forEach { line -> appendLine("\t${line.replaceControlCharacters()}") }
        })
    }
}

/**
 * Similar to [assertMatches] but just returns a boolean value instead of throwing an assertion.
 */
fun TestTerminal.matches(expected: RenderScope.() -> Unit): Boolean {
    return this.resolveRerenders() == TestTerminal.consoleOutputFor(expected)
}
